package com.example.perpetualmotioncardgame.activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.example.perpetualmotioncardgame.R;
import com.example.perpetualmotioncardgame.classes.CardPilesAdapter;
import com.example.perpetualmotioncardgame.classes.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mintedtech.perpetual_motion.pm_game.Card;
import com.mintedtech.perpetual_motion.pm_game.PMGame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.EmptyStackException;


public class MainActivity extends AppCompatActivity
{
    // Game (current game) object
    private PMGame mCurrentGame;

    // Adapter (current board) object
    private CardPilesAdapter mAdapter;

    // UI Strings
    private String mWINNER_MSG, mNON_WINNER_MSG;

    // preference booleans; indicates if these respective settings currently enabled/disabled
    private boolean mPrefUseAutoSave, mPrefShowErrors;

    // Status Bar View references
    private TextView mTv_cardsRemaining, mTv_cardsInDeck;
    private View mSbContainer;
    private Snackbar mSnackBar;

    // General Keys
    private final String mKeyPrefsName = "PREFS";
    private final String mKeyCheckedPiles = "CHECKED_PILES";
    private final String mKeyGame = "GAME";

    // Preference Keys: values are already in strings.xml and will be assigned to these in onCreate
    private String mKeyAutoSave, mKeyShowErrors;

    @Override protected void onSaveInstanceState (@SuppressWarnings("NullableProblems") Bundle outState)
    {
        // call the super-class's method to save fields, etc.
        super.onSaveInstanceState (outState);

        // save current game, which includes the piles from which we derive pile tops after restore
        outState.putString (mKeyGame, PMGame.getJSONof (mCurrentGame));

        // save each checkbox's status so that we can check off those boxes after restore
        outState.putBooleanArray (mKeyCheckedPiles, mAdapter.getCheckedPiles ());
    }

    @Override protected void onStop ()
    {
        saveToSharedPref ();
        super.onStop ();
    }

    private void saveToSharedPref ()
    {
        // Create a SP reference to the prefs file on the device whose name matches mKeyPrefsName
        // If the file on the device does not yet exist, then it will be created
        SharedPreferences preferences = getSharedPreferences (mKeyPrefsName, MODE_PRIVATE);

        // Create an Editor object to write changes to the preferences object above
        SharedPreferences.Editor editor = preferences.edit ();

        // clear whatever was set last time
        editor.clear ();

        // save the settings (Show Errors and Use AutoSave)
        saveSettingsToSharedPrefs (editor);

        // if autoSave is on then save the board
        saveGameAndBoardToSharedPrefsIfAutoSaveIsOn (editor);

        // apply the changes to the XML file in the device's storage
        editor.apply ();
    }


    private void saveSettingsToSharedPrefs (SharedPreferences.Editor editor)
    {
        // save "show/hide turn errors" preference
        editor.putBoolean (mKeyShowErrors, mPrefShowErrors);

        // save "autoSave" preference
        editor.putBoolean (mKeyAutoSave, mPrefUseAutoSave);
    }

    private void saveGameAndBoardToSharedPrefsIfAutoSaveIsOn (SharedPreferences.Editor editor)
    {
        if (mPrefUseAutoSave) {
            final boolean [] checkedPiles = mAdapter.getCheckedPiles ();

            // Game Object
            editor.putString (mKeyGame, PMGame.getJSONof (mCurrentGame));

            // Checks currently checked/unchecked in RecyclerView
            for (int i = 0; i < checkedPiles.length; i++) {
                editor.putBoolean (mKeyCheckedPiles + i, checkedPiles[i]);
            }
        }
    }

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        setupToolbar ();
        setupFAB ();

        PreferenceManager.setDefaultValues (getApplicationContext (), R.xml.root_preferences, true);
        setFieldReferencesToResFileValues ();
        setFieldReferencesToViewsAndSnackBar ();
        setupBoard ();
        restoreAppSettingsFromPrefs ();
        doInitialStartGame (savedInstanceState);
    }

    private void setupToolbar ()
    {
        Toolbar toolbar = findViewById (R.id.toolbar);
        setSupportActionBar (toolbar);
    }

    private void setFieldReferencesToResFileValues ()
    {
        mKeyAutoSave = getString (R.string.key_use_auto_save);
        mKeyShowErrors = getString (R.string.key_show_turn_specific_error_messages);
        mWINNER_MSG = getString (R.string.you_have_cleared_the_board)
                .concat ("\n").concat (getString (R.string.new_game_question));
        mNON_WINNER_MSG = getString (R.string.no_more_turns_remain)
                .concat ("\n").concat (getString (R.string.new_game_question));
    }

    private void setFieldReferencesToViewsAndSnackBar ()
    {
        mSbContainer = findViewById (R.id.activity_main);
        mTv_cardsRemaining = findViewById (R.id.tv_cards_remaining_to_discard);
        mTv_cardsInDeck = findViewById (R.id.tv_cards_in_deck);
        mSnackBar = Snackbar.make (mSbContainer, R.string.welcome_new_game, Snackbar.LENGTH_SHORT);
    }

    private void setupBoard ()
    {
        // create the adapter which will drive the RecyclerView (Model portion of MVC)
        mAdapter = new CardPilesAdapter (getString (R.string.cards_in_stack));

        // Set the listener object to respond to clicks in the RecyclerView
        // clicks are forwarded to the listener by the adapter
        CardPilesAdapter.AdapterOnItemClickListener mClickListener = getNewOnItemClickListener();
        mAdapter.setOnItemClickListener (mClickListener);

        // get a reference to the RecyclerView - not a field because it's not needed elsewhere
        RecyclerView rvPiles = findViewById (R.id.rv_piles);

        // Please note the use of an xml integer here: portrait will be 2x2 and landscape 1x4; neat!
        final int RV_COLUMN_COUNT = getResources ().getInteger (R.integer.rv_columns);

        // optimization setting - since there are always exactly four piles
        rvPiles.setHasFixedSize (true);

        // Create a new LayoutManager object to be used in the RecyclerView
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager
                (this, RV_COLUMN_COUNT);

        // apply the Layout Manager object just created to the RecyclerView
        rvPiles.setLayoutManager (layoutManager);

        // apply the adapter object to the RecyclerView
        rvPiles.setAdapter (mAdapter);
    }

    /**
     * This object of our custom Listener class handles events in the adapter; created here anon.
     * This is called from the Adapter when the user clicks on the board.
     * This leaves the Adapter to handle primarily the Model part of MVC, not (View and) Controller
     * @return an object that responds to clicks inside a RecyclerView whose ViewHolder implements this interface
     */
    private CardPilesAdapter.AdapterOnItemClickListener getNewOnItemClickListener ()
    {
        return new CardPilesAdapter.AdapterOnItemClickListener ()
        {
            public void onItemClick (int position, View view)
            {
                try {
                    if (mCurrentGame.getNumberOfCardsInStackAtPosition (position) > 0) {
                        if (mCurrentGame.isGameOver ()) {
                            showSB_AlreadyGameOver ();
                        }
                        else {
                            dismissSnackBarIfShown ();
                            mAdapter.toggleCheck (position);
                        }
                    }
                    // otherwise, if this stack is empty (and no card shown), then ignore the click
                }
                catch (Exception e) {
                    Log.d ("STACK", "Toggle Crashed: " + e.getMessage ());
                    // No reason for it to crash but if it did...
                }
            }

        };
    }

    private void restoreAppSettingsFromPrefs ()
    {
        // Since this is for reading only, no editor is needed unlike in saveRestoreState
        SharedPreferences preferences = getSharedPreferences (mKeyPrefsName, MODE_PRIVATE);

        // restore AutoSave preference value
        mPrefUseAutoSave = preferences.getBoolean (mKeyAutoSave, true);
        mPrefShowErrors = preferences.getBoolean (mKeyShowErrors, true);
    }

    private void doInitialStartGame (Bundle savedInstanceState)
    {
        // If this is NOT the first run, meaning, we're recreating as a result of a device rotation
        // then restore the board (meaning both cards and user's checks) as from before the rotation
        // Otherwise (if this is a fresh start of the Activity and NOT after a rotation),
        // if auto-save is enabled then restore the game from sharedPrefs
        // otherwise (not post-rotation and auto-save off or no game in prefs): start a new game

        if (savedInstanceState != null) {
            restoreSavedGameAndBoardFromBundle (savedInstanceState);
        }
        else  if (mPrefUseAutoSave && isValidGameInPrefs ()) {
            restoreSavedGameAndBoardFromPrefs ();
        }
        else {
            startNewGame ();
        }
    }

    private boolean isValidGameInPrefs ()
    {
        SharedPreferences preferences = getSharedPreferences (mKeyPrefsName, MODE_PRIVATE);

        // restore the current game
        String savedGame = preferences.getString (mKeyGame, "");
        return (!savedGame.equals (""));
    }

    private void startGameAndSetBoard(PMGame game, boolean[] checks, int msgID)
    {
        // create/restore the game
        mCurrentGame = game != null ? game : new PMGame ();

        // update the board
        doUpdatesAfterGameStartOrTakingTurn ();

        // overwrite checks if not null
        if (checks != null)
            mAdapter.overwriteChecksFrom(checks);

        // Show New Game message if non-zero
        if (msgID !=0)
        {
            mSnackBar = Snackbar.make (mSbContainer, msgID, Snackbar.LENGTH_SHORT);
            mSnackBar.show ();
        }
    }

    private void restoreSavedGameAndBoardFromPrefs ()
    {
        SharedPreferences preferences = getSharedPreferences (mKeyPrefsName, MODE_PRIVATE);

        startGameAndSetBoard (PMGame.restoreGameFromJSON(preferences.getString (mKeyGame, "")),
                getArrayFromPrefsValues (preferences),
                R.string.welcome_restore_game);
    }

    private boolean[] getArrayFromPrefsValues (SharedPreferences preferences)
    {
        // Even if the Adapter's array is empty, we can use it to determine the array length, etc.
        final boolean [] checkedPiles = mAdapter.getCheckedPiles ();

        for (int i = 0; i < checkedPiles.length; i++) {
            checkedPiles[i] = preferences.getBoolean (mKeyCheckedPiles + i, false);
        }
        return checkedPiles;
    }

    private void restoreSavedGameAndBoardFromBundle (Bundle savedInstanceState)
    {
        startGameAndSetBoard (PMGame.restoreGameFromJSON (savedInstanceState.getString (mKeyGame)),
                savedInstanceState.getBooleanArray (mKeyCheckedPiles), 0);
    }

    private void startNewGame ()
    {
        startGameAndSetBoard (new PMGame (), null, R.string.welcome_new_game);
    }

    private void setupFAB ()
    {
        // Display the rules (from Java) when the user clicks on the Floating Action Button
        FloatingActionButton fab = findViewById (R.id.fab);
        fab.setOnClickListener (new View.OnClickListener ()
        {
            @Override
            public void onClick (View view)
            {
                Utils.showInfoDialog (MainActivity.this,"Information", mCurrentGame.getRules ());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater ().inflate (R.menu.menu_main, menu);
        return true;
    }

    @Override public boolean onPrepareOptionsMenu (Menu menu)
    {
        menu.findItem (R.id.action_toggle_auto_save).setChecked (mPrefUseAutoSave);
        menu.findItem (R.id.action_turn_show_error_messages).setChecked (mPrefShowErrors);
        return super.onPrepareOptionsMenu (menu);
    }

    @Override
    public boolean onOptionsItemSelected (@NonNull MenuItem item)
    {
        dismissSnackBarIfShown ();

        switch (item.getItemId ()) {

            case R.id.action_about: {
                showAbout ();
                return true;
            }
            case R.id.action_toggle_auto_save: {
                toggleMenuItem (item);
                mPrefUseAutoSave = item.isChecked ();
                return true;
            }
            case R.id.action_turn_show_error_messages:
                toggleMenuItem (item);
                mPrefShowErrors = item.isChecked ();
                return true;
            default:
                // If the user clicked on some unknown menu item, then the super... will handle that
                return super.onOptionsItemSelected (item);
        }
    }

    private void dismissSnackBarIfShown ()
    {
        if (mSnackBar.isShown ())
            mSnackBar.dismiss ();
    }

    private void toggleMenuItem (MenuItem item)
    {
        item.setChecked (!item.isChecked ());
    }

    private void doUpdatesAfterGameStartOrTakingTurn ()
    {
        updateStatusBar ();
        updateRecyclerViewAdapter ();
        checkForGameOver ();
    }

    private void updateStatusBar ()
    {
        // Update the Status Bar with the number of cards left (from Java) via our current game obj
        mTv_cardsRemaining.setText (getString (R.string.cards_to_discard).concat
                (String.valueOf (mCurrentGame.getNumCardsLeftToDiscardFromDeckAndStacks ())));

        mTv_cardsInDeck.setText (getString (R.string.in_deck).concat (
                String.valueOf (mCurrentGame.getNumberOfCardsLeftInDeck ())));
    }

    private void updateRecyclerViewAdapter ()
    {
        // get the data for the new board from our game object (Java) which tracks the four stacks
        Card[] currentTops = mCurrentGame.getCurrentStacksTopIncludingNulls ();

        // temporary card used when updating the board below
        Card currentCard, currentAdapterCard;

        // Update the board one pile/card at a time, as needed
        for (int i = 0; i < currentTops.length; i++) {
            currentCard = currentTops[i];
            currentAdapterCard = mAdapter.getCardAt (i);

            if ((currentAdapterCard == null || !currentAdapterCard.equals (currentCard))) {
                // Have Adapter set each card to the matching top card of each stack
                mAdapter.updatePile (i, currentCard,
                        mCurrentGame.getNumberOfCardsInStackAtPosition (i));
            }

            // Clear the checks that the user might have just set
            mAdapter.clearCheck (i);
        }
    }


    /**
     * If the game is over, this method outputs a dialog box with the correct message (win/not)
     */
    private void checkForGameOver ()
    {
        // If the game is over, let the user know what happened and then start a new game
        if (mCurrentGame.isGameOver ()) {
            showGameOverDialog (getString (R.string.game_over),
                    mCurrentGame.isWinner () ? mWINNER_MSG : mNON_WINNER_MSG);
        }
    }

    /**
     * This method lets us know what the n checked pile contains (out of only checked piles, not 4)
     *
     * @param checkedPiles 4-element boolean array of four checked/unchecked piles
     * @param position     The checked pile number (0 through x) of the checked pile
     * @return The pile number (out of the checked piles, not out of all 4 piles)
     */
    private int getCheckedItem (boolean[] checkedPiles, int position)
    {
        // create a new int array containing the number of elements == the number of checked cards
        int[] checkedItems = new int[getCountOfChecks (checkedPiles)];

        // i is the index for the 4-element array of all stacks passed in
        // j is the index for the new array of position numbers just created
        for (int i = 0, j = 0; i < checkedPiles.length; i++) {
            // increment j only if current element is true
            if (checkedPiles[i]) {
                checkedItems[j++] = i;
            }

        }
        return checkedItems[position];
    }

    /**
     * This method lets us know how many cards have been checked off
     *
     * @param checkedPiles the array of checked Cards
     * @return the number of cards checked
     */
    private int getCountOfChecks (boolean[] checkedPiles)
    {
        int totalChecked = 0;
        for (boolean checkedPile : checkedPiles) {
            totalChecked += checkedPile ? 1 : 0;
        }
        return totalChecked;
    }

    private void showErrorSB (String msg)
    {
        if (mPrefShowErrors) {
            mSnackBar = Snackbar.make (mSbContainer, msg, Snackbar.LENGTH_LONG);
            mSnackBar.show ();
        }
    }

    private void showSB_AlreadyGameOver ()
    {
        if (mPrefShowErrors) {
            mSnackBar = Snackbar.make (mSbContainer,
                    getString(R.string.msg_game_already_ended),
                    Snackbar.LENGTH_LONG);
            mSnackBar.setAction (R.string.new_game, new View.OnClickListener ()
            {
                @Override public void onClick (View view)
                {
                    startNewGame ();
                }
            });
            mSnackBar.show ();
        }
    }

    private void showSBErrorDiscardOneTwoOrOther (int checkedCount)
    {
        if (mPrefShowErrors) {
            String errorMsg = getString (checkedCount > 2 || checkedCount < 1 ?
                    R.string.turn_error_discard_other :
                    checkedCount == 2 ? R.string.turn_error_discard_two
                            : R.string.turn_error_discard_one);

            mSnackBar = Snackbar.make (mSbContainer, errorMsg, Snackbar.LENGTH_LONG);
            mSnackBar.show ();
        }
    }

    /**
     * This method handles the user's choice to discard either:
     * the lower of two cards of the same suit or two cards of the same rank
     *
     * @param view this is the calling Object (e.g. button): irrelevant to us so we ignore it
     */
    public void turn_action_discard (@SuppressWarnings("unused") View view)
    {
        if (mCurrentGame.isGameOver ()) {
            showSB_AlreadyGameOver ();
        }
        else {
            dismissSnackBarIfShown ();
            final boolean[] checkedPiles = mAdapter.getCheckedPiles ();
            attemptDiscard (checkedPiles, getCountOfChecks (checkedPiles));
        }
    }

    private void attemptDiscard (boolean[] checkedPiles, int countOfChecks)
    {
        try {
            discardOneOrTwo (checkedPiles, countOfChecks);
            doUpdatesAfterGameStartOrTakingTurn ();
        }
        catch (EmptyStackException ese) {
            showErrorSB (getString (R.string.error_cannot_discard_from_empty_pile));
        }
        catch (UnsupportedOperationException uoe) {
            showErrorSB (uoe.getMessage ());
        }
    }

    private void discardOneOrTwo (boolean[] checkedPiles, int countOfChecks)
    {
        int pileTopToDiscard, secondPileTopToDiscard;
        switch (countOfChecks) {
            case 1: {
                pileTopToDiscard = getCheckedItem (checkedPiles, 0);
                mCurrentGame.discardOneLowestOfSameSuit (pileTopToDiscard);
                break;
            }
            case 2: {
                pileTopToDiscard = getCheckedItem (checkedPiles, 0);
                secondPileTopToDiscard = getCheckedItem (checkedPiles, 1);
                mCurrentGame.discardBothOfSameRank (pileTopToDiscard, secondPileTopToDiscard);
                break;
            }
            default: {
                showSBErrorDiscardOneTwoOrOther (countOfChecks);
            }
        }
    }

    /**
     * This method handles the user's choice to deal one card to each stack (i.e. new top of each)
     *
     * @param view this is the calling Object (e.g. button): irrelevant to us so we ignore it
     */
    public void turn_action_deal (@SuppressWarnings ("UnusedParameters") View view)
    {
        if (mCurrentGame.isGameOver ()) {
            showSB_AlreadyGameOver ();
        }
        else {
            try {
                dismissSnackBarIfShown ();
                mCurrentGame.dealOneCardToEachStack ();
            }
            catch (EmptyStackException ese) {
                Utils.showInfoDialog (this, R.string.title_no_cards_remain,
                        R.string.body_all_cards_dealt_to_stacks);
            }

            // cards will remain as above but clear checkboxes either way, even if deck is empty
            doUpdatesAfterGameStartOrTakingTurn ();
        }
    }

    /**
     * Starts a new game when ActionBar button is pressed
     *
     * @param item MenuItem that triggered this call - not relevant to us so it is ignored
     */
    public void startNewGame (@SuppressWarnings ("UnusedParameters") MenuItem item)
    {
        startNewGame ();
    }

    public void undoLastMove (@SuppressWarnings ("UnusedParameters") MenuItem item)
    {
        undoLastMove ();
    }

    private void showAbout ()
    {
        Utils.showInfoDialog (this, R.string.app_name, R.string.about_message);
    }

    private void undoLastMove ()
    {
        try {
            mCurrentGame.undoLatestTurn ();
            doUpdatesAfterGameStartOrTakingTurn ();
        }
        catch (UnsupportedOperationException uoe) {
            Utils.showInfoDialog (this, "Can't Undo", uoe.getMessage ());
        }
    }

    /**
     * Shows an Android (nicer) equivalent to JOptionPane
     *
     * @param strTitle Title of the Dialog box
     * @param strMsg   Message (body) of the Dialog box
     */
    private void showGameOverDialog (String strTitle, String strMsg)
    {
        final DialogInterface.OnClickListener newGameListener =
                new DialogInterface.OnClickListener ()
                {
                    @Override
                    public void onClick (DialogInterface dialog, int which)
                    {
                        startNewGame ();
                    }
                };

        Utils.showYesNoDialog (this, strTitle, strMsg, newGameListener, null);
    }

    // LG work-around for select older devices
    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event)
    {
        boolean isOldLG = ((keyCode == KeyEvent.KEYCODE_MENU) &&
                (Build.VERSION.SDK_INT <= 16) &&
                (Build.MANUFACTURER.compareTo ("LGE") == 0));

        //noinspection SimplifiableConditionalExpression
        return isOldLG ? true : super.onKeyDown (keyCode, event);
    }

    @Override
    public boolean onKeyUp (int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_MENU) &&
                (Build.VERSION.SDK_INT <= 16) &&
                (Build.MANUFACTURER.compareTo ("LGE") == 0)) {
            openOptionsMenu ();
            return true;
        }
        return super.onKeyUp (keyCode, event);
    }
}







/*
public class MainActivity extends AppCompatActivity {

    // Game (current game) object
    private PMGame mCurrentGame;

    // Adapter (current board) object
    private CardPilesAdapter mAdapter;

    // UI Strings
    private String mWINNER_MSG, mNON_WINNER_MSG;

    // preference booleans; indicates if these respective settings currently enabled/disabled
    private boolean mPrefUseAutoSave, mPrefShowErrors;

    // Status Bar View references
    private TextView mTv_cardsRemaining, mTv_cardsInDeck;
    private View mSbContainer;
    private Snackbar mSnackBar;

    // General Keys reading and writing
    private final String mKeyPrefsName = "PREFS";
    private final String mKeyCheckedPiles = "CHECKED_PILES";
    private final String mKeyGame = "GAME";

    // Preference Keys: values are already in strings.xml and will be assigned to these in onCreate
    private String mKeyAutoSave, mKeyShowErrors;


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(mKeyGame, PMGame.getJSONof(mCurrentGame)); // get json of game object and write it to outstate

        outState.putBooleanArray(mKeyCheckedPiles, mAdapter.getCheckedPiles());
    }

    @Override
    protected void onStop() {
        saveToSharedPref();
        super.onStop();
    }

    private void saveToSharedPref() {
        SharedPreferences preferences = getSharedPreferences(mKeyPrefsName, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        saveSettingsToSharedPrefs(editor);
        saveGameAndBoardToSHaredPrefsIfAutoSaveIsOn(editor);


    }

    private void saveGameAndBoardToSHaredPrefsIfAutoSaveIsOn(SharedPreferences.Editor editor) {
        if (mPrefUseAutoSave) {
            final boolean[] checkedPiles = mAdapter.getCheckedPiles();
            editor.putString(mKeyGame, PMGame.getJSONof(mCurrentGame));

            for (int i = 0; i < checkedPiles.length; i++)
                editor.putBoolean(mKeyCheckedPiles + i, checkedPiles[i]);
        }
    }

    private void saveSettingsToSharedPrefs(SharedPreferences.Editor editor) {
        editor.putBoolean(mKeyShowErrors, mPrefShowErrors);
        editor.putBoolean(mKeyAutoSave, mPrefUseAutoSave);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();
        setupFAB();


        // prefs default vals
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.root_preferences, true);
        setFieldReferencesToResFileValues ();
        setFieldReferencesToViewsAndSnackBar ();
        setupBoard ();
        restoreAppSettingsFromPrefs ();
        doInitialStartGame (savedInstanceState);

    }

    private void setupFAB() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
    private void setFieldReferencesToResFileValues ()
    {
        mKeyAutoSave = getString (R.string.key_use_auto_save);
        mKeyShowErrors = getString (R.string.key_show_turn_specific_error_messages);
        mWINNER_MSG = getString (R.string.you_have_cleared_the_board)
                .concat ("\n").concat (getString (R.string.new_game_question));
        mNON_WINNER_MSG = getString (R.string.no_more_turns_remain)
                .concat ("\n").concat (getString (R.string.new_game_question));
    }

    private void setFieldReferencesToViewsAndSnackBar ()
    {
        mSbContainer = findViewById (R.id.activity_main);
        mTv_cardsRemaining = findViewById (R.id.tv_cards_remaining_to_discard);
        mTv_cardsInDeck = findViewById (R.id.tv_cards_in_deck);
        mSnackBar = Snackbar.make (mSbContainer, R.string.welcome_new_game, Snackbar.LENGTH_SHORT);
    }

    private void setupBoard ()
    {
        // create the adapter which will drive the RecyclerView (Model portion of MVC)
        mAdapter = new CardPilesAdapter (getString (R.string.cards_in_stack));

        // Set the listener object to respond to clicks in the RecyclerView
        // clicks are forwarded to the listener by the adapter
        CardPilesAdapter.AdapterOnItemClickListener mClickListener = getNewOnItemClickListener();
        mAdapter.setOnItemClickListener (mClickListener);

        // get a reference to the RecyclerView - not a field because it's not needed elsewhere
        RecyclerView rvPiles = findViewById (R.id.rv_piles);

        // Please note the use of an xml integer here: portrait will be 2x2 and landscape 1x4; neat!
        final int RV_COLUMN_COUNT = getResources ().getInteger (R.integer.rv_columns);

        // optimization setting - since there are always exactly four piles
        rvPiles.setHasFixedSize (true);

        // Create a new LayoutManager object to be used in the RecyclerView
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager
                (this, RV_COLUMN_COUNT);

        // apply the Layout Manager object just created to the RecyclerView
        rvPiles.setLayoutManager (layoutManager);

        // apply the adapter object to the RecyclerView
        rvPiles.setAdapter (mAdapter);
    }


    */
/**
     * This object of our custom Listener class handles events in the adapter; created here anon.
     * This is called from the Adapter when the user clicks on the board.
     * This leaves the Adapter to handle primarily the Model part of MVC, not (View and) Controller
     * @return an object that responds to clicks inside a RecyclerView whose ViewHolder implements this interface
     *//*

    private CardPilesAdapter.AdapterOnItemClickListener getNewOnItemClickListener ()
    {
        return new CardPilesAdapter.AdapterOnItemClickListener ()
        {
            public void onItemClick (int position, View view)
            {
                try {
                    if (mCurrentGame.getNumberOfCardsInStackAtPosition (position) > 0) {
                        if (mCurrentGame.isGameOver ()) {
                            showSB_AlreadyGameOver ();
                        }
                        else {
                            dismissSnackBarIfShown ();
                            mAdapter.toggleCheck (position);
                        }
                    }
                    // otherwise, if this stack is empty (and no card shown), then ignore the click
                }
                catch (Exception e) {
                    Log.d ("STACK", "Toggle Crashed: " + e.getMessage ());
                    // No reason for it to crash but if it did...
                }
            }

        };
    }

    private void showSB_AlreadyGameOver ()
    {
        if (mPrefShowErrors) {
            mSnackBar = Snackbar.make (mSbContainer,
                    getString(R.string.msg_game_already_ended),
                    Snackbar.LENGTH_LONG);
            mSnackBar.setAction (R.string.new_game, new View.OnClickListener ()
            {
                @Override public void onClick (View view)
                {
                    startNewGame ();
                }
            });
            mSnackBar.show ();
        }
    }



    private void restoreAppSettingsFromPrefs ()
    {
        // Since this is for reading only, no editor is needed unlike in saveRestoreState
        SharedPreferences preferences = getSharedPreferences (mKeyPrefsName, MODE_PRIVATE);

        // restore AutoSave preference value
        mPrefUseAutoSave = preferences.getBoolean (mKeyAutoSave, true);
        mPrefShowErrors = preferences.getBoolean (mKeyShowErrors, true);
    }

    private void doInitialStartGame (Bundle savedInstanceState)
    {
        // If this is NOT the first run, meaning, we're recreating as a result of a device rotation
        // then restore the board (meaning both cards and user's checks) as from before the rotation
        // Otherwise (if this is a fresh start of the Activity and NOT after a rotation),
        // if auto-save is enabled then restore the game from sharedPrefs
        // otherwise (not post-rotation and auto-save off or no game in prefs): start a new game

        if (savedInstanceState != null) {
            restoreSavedGameAndBoardFromBundle (savedInstanceState);
        }
        else  if (mPrefUseAutoSave && isValidGameInPrefs ()) {
            restoreSavedGameAndBoardFromPrefs ();
        }
        else {
            startNewGame ();
        }
    }

    private boolean isValidGameInPrefs ()
    {
        SharedPreferences preferences = getSharedPreferences (mKeyPrefsName, MODE_PRIVATE);

        // restore the current game
        String savedGame = preferences.getString (mKeyGame, "");
        return (!savedGame.equals (""));
    }

    private void startGameAndSetBoard(PMGame game, boolean[] checks, int msgID)
    {
        // create/restore the game
        mCurrentGame = game != null ? game : new PMGame ();

        // update the board
        doUpdatesAfterGameStartOrTakingTurn ();

        // overwrite checks if not null
        if (checks != null)
            mAdapter.overwriteChecksFrom(checks);

        // Show New Game message if non-zero
        if (msgID !=0)
        {
            mSnackBar = Snackbar.make (mSbContainer, msgID, Snackbar.LENGTH_SHORT);
            mSnackBar.show ();
        }
    }

    private void restoreSavedGameAndBoardFromPrefs ()
    {
        SharedPreferences preferences = getSharedPreferences (mKeyPrefsName, MODE_PRIVATE);

        startGameAndSetBoard (PMGame.restoreGameFromJSON(preferences.getString (mKeyGame, "")),
                getArrayFromPrefsValues (preferences),
                R.string.welcome_restore_game);
    }

    private boolean[] getArrayFromPrefsValues (SharedPreferences preferences)
    {
        // Even if the Adapter's array is empty, we can use it to determine the array length, etc.
        final boolean [] checkedPiles = mAdapter.getCheckedPiles ();

        for (int i = 0; i < checkedPiles.length; i++) {
            checkedPiles[i] = preferences.getBoolean (mKeyCheckedPiles + i, false);
        }
        return checkedPiles;
    }

    private void restoreSavedGameAndBoardFromBundle (Bundle savedInstanceState)
    {
        startGameAndSetBoard (PMGame.restoreGameFromJSON (savedInstanceState.getString (mKeyGame)),
                savedInstanceState.getBooleanArray (mKeyCheckedPiles), 0);
    }


    public void undoLastMove (@SuppressWarnings ("UnusedParameters") MenuItem item)
    {
        undoLastMove ();
    }

    private void undoLastMove ()
    {
        try {
            mCurrentGame.undoLatestTurn ();
            doUpdatesAfterGameStartOrTakingTurn ();
        }
        catch (UnsupportedOperationException uoe) {
            Utils.showInfoDialog (this, "Can't Undo", uoe.getMessage ());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


//    When user hits discard
    public void turn_action_discard(View view) {

    }


//    When user hits deal button
    public void turn_action_deal(View view) {
    }

    @Override public boolean onPrepareOptionsMenu (Menu menu)
    {
        menu.findItem (R.id.action_toggle_auto_save).setChecked (mPrefUseAutoSave);
        menu.findItem (R.id.action_turn_show_error_messages).setChecked (mPrefShowErrors);
        return super.onPrepareOptionsMenu (menu);
    }

    @Override
    public boolean onOptionsItemSelected (@NonNull MenuItem item)
    {
        dismissSnackBarIfShown ();

        switch (item.getItemId ()) {

            case R.id.action_about: {
                showAbout ();
                return true;
            }
            case R.id.action_toggle_auto_save: {
                toggleMenuItem (item);
                mPrefUseAutoSave = item.isChecked ();
                return true;
            }
            case R.id.action_turn_show_error_messages:
                toggleMenuItem (item);
                mPrefShowErrors = item.isChecked ();
                return true;
            default:
                // If the user clicked on some unknown menu item, then the super... will handle that
                return super.onOptionsItemSelected (item);
        }
    }


    private void showAbout ()
    {
        Utils.showInfoDialog (this, R.string.app_name, R.string.about_message);
    }

    private void dismissSnackBarIfShown ()
    {
        if (mSnackBar.isShown ())
            mSnackBar.dismiss ();
    }

    private void toggleMenuItem (MenuItem item)
    {
        item.setChecked (!item.isChecked ());
    }

    private void doUpdatesAfterGameStartOrTakingTurn ()
    {
        updateStatusBar ();
        updateRecyclerViewAdapter ();
        checkForGameOver ();
    }


    */
/**
     * If the game is over, this method outputs a dialog box with the correct message (win/not)
     *//*

    private void checkForGameOver ()
    {
        // If the game is over, let the user know what happened and then start a new game
        if (mCurrentGame.isGameOver ()) {
            showGameOverDialog (getString (R.string.game_over),
                    mCurrentGame.isWinner () ? mWINNER_MSG : mNON_WINNER_MSG);
        }
    }


    */
/**
     * Shows an Android (nicer) equivalent to JOptionPane
     *
     * @param strTitle Title of the Dialog box
     * @param strMsg   Message (body) of the Dialog box
     *//*

    private void showGameOverDialog (String strTitle, String strMsg)
    {
        final DialogInterface.OnClickListener newGameListener =
                new DialogInterface.OnClickListener ()
                {
                    @Override
                    public void onClick (DialogInterface dialog, int which)
                    {
                        startNewGame ();
                    }
                };

        Utils.showYesNoDialog (this, strTitle, strMsg, newGameListener, null);
    }

    private void updateStatusBar ()
    {
        // Update the Status Bar with the number of cards left (from Java) via our current game obj
        mTv_cardsRemaining.setText (getString (R.string.cards_to_discard).concat
                (String.valueOf (mCurrentGame.getNumCardsLeftToDiscardFromDeckAndStacks ())));

        mTv_cardsInDeck.setText (getString (R.string.in_deck).concat (
                String.valueOf (mCurrentGame.getNumberOfCardsLeftInDeck ())));
    }

    private void updateRecyclerViewAdapter ()
    {
        // get the data for the new board from our game object (Java) which tracks the four stacks
        Card[] currentTops = mCurrentGame.getCurrentStacksTopIncludingNulls ();

        // temporary card used when updating the board below
        Card currentCard, currentAdapterCard;

        // Update the board one pile/card at a time, as needed
        for (int i = 0; i < currentTops.length; i++) {
            currentCard = currentTops[i];
            currentAdapterCard = mAdapter.getCardAt (i);

            if ((currentAdapterCard == null || !currentAdapterCard.equals (currentCard))) {
                // Have Adapter set each card to the matching top card of each stack
                mAdapter.updatePile (i, currentCard,
                        mCurrentGame.getNumberOfCardsInStackAtPosition (i));
            }

            // Clear the checks that the user might have just set
            mAdapter.clearCheck (i);
        }
    }

    private void startNewGame ()
    {
        startGameAndSetBoard (new PMGame (), null, R.string.welcome_new_game);
    }

    */
/**
     * Starts a new game when ActionBar button is pressed
     *
     * @param item MenuItem that triggered this call - not relevant to us so it is ignored
     *//*

    public void startNewGame (@SuppressWarnings ("UnusedParameters") MenuItem item)
    {
        startNewGame ();
    }

}
*/
