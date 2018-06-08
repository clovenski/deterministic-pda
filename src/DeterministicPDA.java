/* 
 * Class DeterministicPDA represents a deteministic push-down automata machine that can be used
 * to process a string of characters of a specified alphabet. It handles exceptions where a
 * transition to be added to the description of the PDA would result in it becoming a non-deterministic
 * PDA.
 * Author: Joel Tengco
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

public class DeterministicPDA {
    // adjacency list for the machine states
    private ArrayList<LinkedList<TransitionGroup>> adjList;
    // number of states in the PDA
    private final int PDA_SIZE;
    // alphabet of the machine
    private ArrayList<Character> alphabet;
    // stack to be used by the machine
    private Stack<Character> stack;
    // initial stack symbol, representing the bottom of the stack
    private final char INIT_STACK_SYMBOL = '$';
    // current state of the machine
    private int currentState;
    // true if the current state is a trap state, false otherwise
    private boolean trapped;
    // set of final, accepting states
    private ArrayList<Integer> finalStates;

    public DeterministicPDA(int size, String alphabetString) {
        // initialize adjacency list of the machine
        PDA_SIZE = size;
        adjList = new ArrayList<LinkedList<TransitionGroup>>(PDA_SIZE);
        for(int i = 0; i < PDA_SIZE; i++)
            adjList.add(new LinkedList<TransitionGroup>());

        // initialize alphabet
        alphabet = new ArrayList<Character>();
        char newSymbol;
        for(int i = 0; i < alphabetString.length(); i++) {
            newSymbol = alphabetString.charAt(i);
            if(!alphabet.contains(newSymbol))
                alphabet.add(newSymbol);
        }

        // initialize machine's stack
        stack = new Stack<Character>();
        stack.push(INIT_STACK_SYMBOL);

        currentState = 0;

        trapped = false;

        finalStates = new ArrayList<Integer>();
    }

    public void addFinalStates(int[] stateNumbers) {
        int newFinalState;

        // for every integer in the given array
        for(int i = 0; i < stateNumbers.length; i++) {
            newFinalState = stateNumbers[i];
            // if the integer is within [0, PDA_SIZE) and not in the finalStates list, add that integer to finalStates list
            if(newFinalState >= 0 && newFinalState < PDA_SIZE && !finalStates.contains(newFinalState))
                finalStates.add(newFinalState);
        }
    }

    public void addTransition(int source, int target, char input, char pop, String push) throws IllegalArgumentException {
        // if source or target is not within [0, PDA_SIZE) or the input symbol given is not lambda and not within the alphabet,
        //      throw an appropriate exception
        if(source < 0 || source >= PDA_SIZE)
            throw new IllegalArgumentException("Invalid source state given.");
        if(target < 0 || target >= PDA_SIZE)
            throw new IllegalArgumentException("Invalid target state given.");
        if(!alphabet.contains(input) && input != '.')
            throw new IllegalArgumentException("Invalid input character given.");

        boolean inputIsLambda = input == '.';
        boolean popIsLambda = pop == '.';

        if(inputIsLambda && popIsLambda)
            throw new IllegalArgumentException("Lambda transition not allowed in deterministic PDA.");

        TransitionGroup targetGroup = null;
        TransitionGroup currentGroup;
        Transition newTransition = new Transition(input, pop, push);

        // for every transition group the source state has, check if the new transition applies to any of them, checking for non-determinism
        //      conflicts along the way
        for(int i = 0; i < adjList.get(source).size(); i++) {
            currentGroup = adjList.get(source).get(i);
            // if the new transition's target state number applies to this current group's target state number,
            //      mark this group as the target group to add the new transition into
            if(currentGroup.targetStateNum == target)
                targetGroup = currentGroup;

            // check for non-determinism conflicts
            for(int j = 0; j < currentGroup.transitions.size(); j++) {
                // if this current group of transitions contains a transition equivalent to the new one, PDA will become non-deterministic;
                //      transitions are defined to be equivalent iff they have the same input symbols and pop symbols
                if(currentGroup.transitions.contains(newTransition))
                    throw new IllegalArgumentException("Adding this transition will make the PDA non-deterministic.");

                // if the new transition has lambda as the input, no other transition with the same pop symbol can already exist
                if(inputIsLambda) {
                    for(char symbol : alphabet)
                        if(currentGroup.transitions.contains(new Transition(symbol, pop, push)))
                            throw new IllegalArgumentException("Adding this transition will make the PDA non-deterministic.");

                // else just need to make sure a transition with lambda as input and the same pop symbol does not already exist
                } else if(currentGroup.transitions.contains(new Transition('.', pop, push)))
                    throw new IllegalArgumentException("Adding this transition will make the PDA non-deterministic.");

                // if the new transition has lambda as pop symbol, no other transition with the same input symbol can already exist
                if(popIsLambda) {
                    for(char symbol : alphabet)
                        if(currentGroup.transitions.contains(new Transition(input, symbol, push)))
                            throw new IllegalArgumentException("Adding this transition will make the PDA non-deterministic.");

                // else just need to make sure a transition with the same input symbol and lambda as pop symbol does not already exist
                } else if(currentGroup.transitions.contains(new Transition(input, '.', push)))
                    throw new IllegalArgumentException("Adding this transition will make the PDA non-deterministic.");
            }
        }

        // if a transition group that the new transition applies to exists
        if(targetGroup != null)
            targetGroup.transitions.add(newTransition);
        else // create a new transition group for this source state, adding the new transition to it
            adjList.get(source).add(new TransitionGroup(target, newTransition));
    }

    // processes the machine with the given input character
    public void readCharacter(char input) throws IllegalArgumentException {
        // if the given input character is not within this machine's alphabet
        if(!alphabet.contains(input))
            throw new IllegalArgumentException(String.format("'%c' is not in this machine's alphabet.", input));

        // if the current state is a trap state, do not process anything and just return
        if(trapped)
            return;

        TransitionGroup currentGroup;
        Transition targetTransition = null;
        int targetIndex;
        int targetState = -1;
        // for every transition group the current state has, check if it contains a transition appropriate for
        //      the given input character and top element of the stack
        for(int i = 0; i < adjList.get(currentState).size(); i++) {
            currentGroup = adjList.get(currentState).get(i);
            // targetIndex will have a value that is not -1 if an appropriate transition exists with the appropriate
            //      input character and pop element
            targetIndex = currentGroup.transitions.indexOf(new Transition(input, stack.peek(), ""));

            // also check for transitions with lambda as pop symbol if the above resulted in targetIndex = -1
            if(targetIndex == -1)
                targetIndex = currentGroup.transitions.indexOf(new Transition(input, '.', ""));

            // if an appropriate transition is found within this group of transitions
            if(targetIndex != -1) {
                targetTransition = currentGroup.transitions.get(targetIndex);
                targetState = currentGroup.targetStateNum;
                break;
            }
        }

        // if an appropriate transition exists
        if(targetTransition != null)
            processTransition(targetState, targetTransition);
        else
            trapped = true;
    }

    private void processTransition(int newCurrentState, Transition transition) {
        // ensure that a valid newCurrentState is given
        assert newCurrentState != -1;
        currentState = newCurrentState;

        // if the transition requires the machine to pop its top element
        if(transition.popElement != '.') {
            // ensure that the top element of the stack matches what the transition
            //      is trying to pop
            assert stack.peek() == transition.popElement;
            stack.pop();
        }

        String pushString = transition.pushElement;
        // if the transition requires to push a string to the stack, do so
        if(!pushString.equals("."))
            for(int i = pushString.length() - 1; i >= 0; i--)
                stack.push(pushString.charAt(i));
    }

    public String getCurrentStatus() {
        if(trapped)
            return "trapped";

        String status = currentState + ":";
        for(int i = stack.size() - 1; i >= 0; i--)
            status += stack.get(i);

        // return "[current_state]:[stack_elements]"
        return status;
    }

    // get the machine's final status as if no more input characters are to
    //      be consumed
    // note: the machine can still process input characters after this method is called;
    //      to reset the machine, invoke the reset() method
    public String getFinalStatus() {
        // if the machine is not trapped and the current state is a final state, accept the string, reject otherwise
        if(!trapped && finalStates.contains(currentState))
            return "String accepted.";
        else
            return "String rejected.";
    }

    // resets the machine;  current state is now the initial state,
    //                      the stack is cleared, with the initial stack symbol being the only element
    //                      the machine is now not trapped if it was previously
    public void reset() {
        currentState = 0;
        stack.clear();
        stack.push(INIT_STACK_SYMBOL);
        trapped = false;
    }

    public boolean inTrappedState() {
        return trapped;
    }

    // represents a group of transitions that all correspond to the same
    //      target state number
    private class TransitionGroup {
        private int targetStateNum;
        private ArrayList<Transition> transitions;

        public TransitionGroup(int targetStateNum, Transition newTransition) {
            this.targetStateNum = targetStateNum;
            transitions = new ArrayList<Transition>();
            transitions.add(newTransition);
        }
    }

    // represents the transitions this machine has, each having a given character to consume,
    //      an element to pop off the stack and a string to push onto the stack
    // note: the target state number that each transition corresponds to is implicitly defined
    //      in the TransitionGroup object that contains that transition
    private class Transition {
        private char charConsumed;
        private char popElement;
        private String pushElement;

        public Transition(char charConsumed, char popElement, String pushElement) {
            this.charConsumed = charConsumed;
            this.popElement = popElement;
            this.pushElement = pushElement;
        }

        // this transition equals another transition iff their respective input symbols and pop symbols match
        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Transition) {
                Transition otherTransition = (Transition)obj;
                return charConsumed == otherTransition.charConsumed
                    && popElement == otherTransition.popElement;
            } else
                return false;
            
        }

        // needed since overriding equals() method from class Object
        @Override
        public int hashCode() {
            return charConsumed + popElement;
        }
    }
}