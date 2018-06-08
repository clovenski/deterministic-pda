/*
 * This class is strictly used to provide an example of using the class DeterminsticPDA.
 * Author: Joel Tengco
 */

import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        DeterministicPDA machine;
        Scanner input = new Scanner(System.in);
        StringTokenizer tokenizer;
        int machineSize;
        String alphabetString;

        // until a valid machine size and alphabet string is given by the user
        while(true) {
            System.out.println("Enter the number of states for this DPDA, then whitespace, then a string containing the alphabet:");
            try {
                tokenizer = new StringTokenizer(input.nextLine());
                if(tokenizer.countTokens() <= 1)
                    throw new IllegalArgumentException("Please enter two inputs.");

                machineSize = Integer.parseInt(tokenizer.nextToken());
                if(machineSize <= 0)
                    throw new IllegalArgumentException("Number of states must be positive.");

                alphabetString = tokenizer.nextToken();
                break;
            } catch(NumberFormatException nfe) {
                System.err.println("Error: Please enter an integer for the number of states.");
            } catch(IllegalArgumentException iae) {
                System.err.println("Error: " + iae.getMessage());
            } catch(Exception e) {
                System.err.println(e.getMessage());
            }
        }

        // create the machine
        machine = new DeterministicPDA(machineSize, alphabetString);

        int sourceState;
        char inputSymbol;
        char popSymbol;
        int targetState;
        String pushString;
        String temp;

        // until the user inputs -1 to indicate they are done, add any valid transition descriptions given to the machine
        while(true) {
            System.out.println("Enter transitions for this machine, or enter -1 to indicate you are done:");
            try {
                tokenizer = new StringTokenizer(input.nextLine());

                if(tokenizer.countTokens() == 1 && tokenizer.nextToken().equals("-1"))
                    break;

                if(tokenizer.countTokens() != 5)
                    throw new IllegalArgumentException("Invalid number of arguments given.");

                sourceState = Integer.parseInt(tokenizer.nextToken());

                temp = tokenizer.nextToken();
                if(temp.length() > 1)
                    throw new IllegalArgumentException("Invalid length for input symbol argument.");
                else
                    inputSymbol = temp.charAt(0);

                temp = tokenizer.nextToken();
                if(temp.length() > 1)
                    throw new IllegalArgumentException("Invalid length for pop symbol argument.");
                else
                    popSymbol = temp.charAt(0);

                targetState = Integer.parseInt(tokenizer.nextToken());

                pushString = tokenizer.nextToken();

                machine.addTransition(sourceState, targetState, inputSymbol, popSymbol, pushString);
            } catch(NumberFormatException nfe) {
                System.err.println("Error: Please enter an integer for the states.");
            } catch(IllegalArgumentException iae) {
                System.err.println("Error: " + iae.getMessage());
            } catch(Exception e) {
                System.err.println(e.getMessage());
            }
        }

        boolean done = false;
        ArrayList<Integer> finalStates = new ArrayList<Integer>();
        int tempInteger;
        int[] tempArray;
        System.out.println("Enter the final states, separated by whitespace, and ending with -1 to indicate you are done (non-integers are ignored):");
        // until -1 is encountered as a token in the user's input, add any valid state numbers to the machine's set of final states if it did not already exist
        while(!done) {
            tokenizer = new StringTokenizer(input.nextLine());
            while(tokenizer.hasMoreTokens()) {
                try {
                    tempInteger = Integer.parseInt(tokenizer.nextToken());

                    if(tempInteger == -1) {
                        done = true;
                        break;
                    }

                    finalStates.add(tempInteger);
                } catch(NumberFormatException nfe) {
                    continue;
                }
            }

            if(finalStates.size() > 0) {
                tempArray = new int[finalStates.size()];
                for(int i = 0; i < finalStates.size(); i++)
                    tempArray[i] = finalStates.get(i);

                machine.addFinalStates(tempArray);

                finalStates.clear();
            }
        }

        System.out.println();

        char inputChar;
        // until the user inputs '.' as input or the machine is in a trapped state, process the machine with the user's first
        //      non-whitespace character inputted, as in when the user inputs "alongstring", 'a' is processed in the machine
        //      with the rest of the string discarded
        while(true) {
            System.out.printf("Current status %s, Enter input: ", machine.getCurrentStatus());
            try {
                tokenizer = new StringTokenizer(input.nextLine());
                if(tokenizer.countTokens() == 0)
                    continue;

                inputChar = tokenizer.nextToken().charAt(0);

                if(inputChar == '.')
                    break;

                machine.readCharacter(inputChar);

                if(machine.inTrappedState())
                    break;
            } catch(IllegalArgumentException iae) {
                System.err.println("Error: " + iae.getMessage() + "\n");
            }
        }

        System.out.println(machine.getFinalStatus());
        machine.reset();
        input.close();
    }
}