package com.gempukku.swccgo.logic.decisions;

import java.util.ArrayList;

/**
 * A decision that involves choosing from a choice of string values on the User Interface.
 */
public abstract class MultipleChoiceAwaitingDecision extends AbstractAwaitingDecision {
    private String[] _possibleResults;

    /**
     * Creates a decision that involves choosing from a choice of string values on the User Interface.
     * @param text the text to show the player making the decision
     * @param possibleResults the strings to choose from
     */
    public MultipleChoiceAwaitingDecision(String text, String[] possibleResults) {
        this(text, possibleResults, -1);
    }

    /**
     * Creates a decision that involves choosing from a choice of string values on the User Interface.
     * @param text the text to show the player making the decision
     * @param possibleResults the strings to choose from, as ArrayList
     */
    public MultipleChoiceAwaitingDecision(String text, ArrayList<String> possibleResults) {
        this(text, convertToStringArray(possibleResults) , -1);
    }

    private static String[] convertToStringArray(ArrayList<String> arrayList) {
        String[] array = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            array[i] = arrayList.get(i);
        }
        return array;
    }

    /**
     * Creates a decision that involves choosing from a choice of string values on the User Interface.
     * @param text the text to show the player making the decision
     * @param possibleResults the strings to choose from
     * @param defaultIndex the index of the default choice, or -1 if no default
     */
    public MultipleChoiceAwaitingDecision(String text, String[] possibleResults, int defaultIndex) {
        super(1, text, AwaitingDecisionType.MULTIPLE_CHOICE);
        _possibleResults = possibleResults;
        setParam("results", _possibleResults);
        setParam("defaultIndex", String.valueOf(defaultIndex));
    }

    /**
     * Sets the string values to choose from.
     * @param possibleResults the strings to choose from
     */
    protected void setPossibleResults(String[] possibleResults) {
        _possibleResults = possibleResults;
        setParam("results", _possibleResults);
    }

    @Override
    public final void decisionMade(String result) throws DecisionResultInvalidException {
        if (result == null)
            throw new DecisionResultInvalidException();

        int index;
        try {
            index = Integer.parseInt(result);
        } catch (NumberFormatException exp) {
            throw new DecisionResultInvalidException("Unknown response number");
        }
        // F1 (2026-07-13): validate the ordinal range BEFORE indexing. An out-of-range
        // wire ordinal used to index _possibleResults directly and throw unchecked
        // ArrayIndexOutOfBoundsException, escaping the mediator's checked
        // DecisionResultInvalidException catch and stranding AI scheduling
        // (Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md, F1; audit P0 #1).
        if (index < 0 || index >= _possibleResults.length)
            throw new DecisionResultInvalidException("Unknown response number");
        validDecisionMade(index, _possibleResults[index]);
    }

    /**
     * This method is called when the string is chosen.
     * @param index the index of the string chosen
     * @param result the string chosen
     */
    protected abstract void validDecisionMade(int index, String result);
}
