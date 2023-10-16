/*
 * Copyright 2019 This source file is part of the Máni open source project
 *
 * Copyright (c) 2018 - 2019.
 *
 * Licensed under Mozilla Public License 2.0
 *
 * See https://github.com/mani-language/Mani/blob/master/LICENSE.md for license information.
 */

package com.mani.lang.main;

import com.mani.lang.exceptions.GeneralError;
import com.mani.lang.token.Token;
import com.mani.lang.token.TokenType;
import com.mani.lang.core.*;
import com.mani.lang.exceptions.RuntimeError;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

public class Mani {
    public static boolean hadError = false;
    public static boolean hadRuntimeError = false;
    public static boolean hasInternet = false;
    public static boolean isStrictMode = false;
    public static boolean compiledMode = false;

    public static String latestErrorMsg = null;

    private static final Interpreter interpreter = new Interpreter();

    /**
     * The core of all cores. This is where the magic begins.
     *
     * Looks for internet, this will setup the STDLIB to either use
     * online version, or local if setup.
     *
     * Then checks args to see if we are running the REPL or processing a file.g
     * @param args
     */
    public static void main(String[] args) {
            System.out.println("Yay inside main " + args.length);
            hasInternet = checkInternet();

            if(args.length > 1) {
                System.out.println("Usage mani [Script.mni]");
            } else if (args.length == 2) {
                runFile(args[1]);
            } else if (!compiledMode) {
                System.out.println("Right before runPrompt");
                runPrompt();
            }

    }

    /**
     * Gets a stdlib url based on the repo/branch it is being run from
     * If not on CI, will use default origin master repo for stdlib
     *
     * @return the url beneath which stdlib is found
     */
    public static String getStdLibURL() {
        String stdlibURL = "https://raw.githubusercontent.com/Mani-Language/Mani/master/stdlib/";
        if ("true".equals(System.getenv("CI"))){
            String sourceUser = System.getenv("CIRCLE_PROJECT_USERNAME");
            String sourceRepo = System.getenv("CIRCLE_PROJECT_REPONAME");
            String sourceBranch = System.getenv("CIRCLE_BRANCH");
            stdlibURL = "https://raw.githubusercontent.com/" + sourceUser + "/" + sourceRepo + "/" + sourceBranch + "/stdlib/";
        }
        return stdlibURL;
    }

    /**
     * Used to figure out if we are connected to the internet or not.
     *
     * @return internetStatus (boolean)
     */
    private static boolean checkInternet() {
        try {
            final URL url = new URL("http://www.github.com");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Used to load a file into its bytes, before processing.
     * @param path
     */
    private static void runFile(String path) {
        if(path.endsWith(".mni")) {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(path));
                String fileName = path;
                if (path.contains("/")) {
                    fileName = path.substring(path.lastIndexOf('/') + 1);
                }
                run(new String(bytes, Charset.defaultCharset()), fileName);
                if(hadError) System.exit(65);
            } catch(NoSuchFileException e) {
                printAndStoreError(path + ": File not Found");
            } catch(IOException e) {
                printAndStoreError(e.getMessage());
            }
        } else {
            printAndStoreError("Mani scripts must end with '.mni'.");
        }
    }

    /**
     * This is our REPL. It is basic, so please be nice.
     */
    private static void runPrompt() {
        System.out.println("The \u001B[36mMani\033[0m Programming language");
        try{
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader reader = new BufferedReader(input);

            for(;;) {
                System.out.print(">> ");
                run(reader.readLine(), "REPL");
                hadError = false;
            }
        } catch(IOException e) {
            printAndStoreError(e.getMessage());
        }
    }

    /**
     * Used for processing files.
     * Takes the file and runs it through the following steps.
     * - Lexer
     * - Lexer verify
     * - Parser
     * - Run statements.
     * - Resolver
     * - Run statements.
     * - Interpreter
     * @param source
     * @param fileName the name of the file being run
     */
    public static void run(String source, String fileName) {
        Lexer lexer = new Lexer(source, fileName);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        if(hadError) return;
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        if(hadError) return;
        interpreter.interpret(statements);

    }

    public static boolean fileExists(String fName) {
        File f;
        if (compiledMode) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream stream = classLoader.getResourceAsStream(fName);
            return (stream != null);
        } else {
            f = new File(fName);
            return f.exists();
        }

    }

    public static File internalFile(String fName) {
        //File f = new File((String)res);
        File f;
        if (compiledMode) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            //InputStream inputStream = classLoader.getResourceAsStream(fName);
            f = new File(classLoader.getResource(fName).getFile());
        } else {
            f = new File(fName);
        }

        return f;
    }


    public static byte[] readFileToByteArray(File toRead) throws IOException {
        if (!compiledMode) {
            return Files.readAllBytes(toRead.toPath());
        } else {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream stream = classLoader.getResourceAsStream(toRead.getName());
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String inputString = "";
            String line = reader.readLine();
            while (line != null) {
                inputString += line + "\n";
                line = reader.readLine();
            }
            return inputString.getBytes();
        }
    }

    /**
     * Used for reporting an error, with the line and message.
     * @param line
     * @param where
     * @param message
     */
    public static void error(int line, String where, String message) {
        report(line, where, message);
    }

    /**
     * Used for reporting an error, with token and message.
     * @param token
     * @param message
     */
    public static void error(Token token, String message) {
        if(token.type == TokenType.EOF) {
            report(token.line, "at end of " + token.file, message);
        } else {
            report(token.line, "at '" + token.lexeme + "' " + token.file , message);
        }
    }

    /**
     * Used by `error` to print the message to the console.
     * @param line
     * @param where
     * @param message
     */
    private static void report(int line, String where, String message) {
        printAndStoreError("[line " + line + "] Error " + where +" : " + message );
        hadError = true;
    }

    /**
     * Used for handling runtime errors, and printing them
     * in the console.
     * @param error
     */
    public static void runtimeError(RuntimeError error) {
        printAndStoreError(error.getMessage() + "\n[line " + error.token.line + "] at " + error.token.file);
        hadRuntimeError = true;
    }

    public static void generalError(GeneralError error) {
        printAndStoreError(error.getMessage());
    }

    public static void printAndStoreError(String errorMsg) {
        latestErrorMsg = errorMsg;
        System.err.println(errorMsg);
    }


}
