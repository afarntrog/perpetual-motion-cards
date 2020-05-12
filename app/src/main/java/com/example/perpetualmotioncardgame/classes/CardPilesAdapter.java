package com.example.perpetualmotioncardgame.classes;

import android.animation.Animator;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.perpetualmotioncardgame.R;
import com.mintedtech.perpetual_motion.pm_game.Card;

import java.util.Locale;


public class CardPilesAdapter extends RecyclerView.Adapter<CardPilesAdapter.ViewHolder> {

    // What feeds the adapter the data?? This will be the array of piletops

    // Listener class to pass click event and data back to Activity
    private static AdapterOnItemClickListener sAdapterOnItemClickListener;

    // An array of 4 cards
    private final Card[] mPILE_TOPS;

    // checked piles, needed for when user clicks discard
    private final boolean[] mCHECKED_PILES;

    // how many total cards are in each pile, needed for when num is 0 then hide top card
    private final int[] mNUMBER_OF_CARDS_IN_PILE;

    // message under each card
    private final String mMSG_CARDS_IN_STACK;


    public void setOnItemClickListener(AdapterOnItemClickListener adapterOnItemClickListener) {
        CardPilesAdapter.sAdapterOnItemClickListener = adapterOnItemClickListener;
    }


    // constructor
    public CardPilesAdapter(String msgCardsInStack) {
        final int NUMBER_OF_PILES = 4;
        mPILE_TOPS = new Card[NUMBER_OF_PILES];
        mCHECKED_PILES = new boolean[NUMBER_OF_PILES];
        mNUMBER_OF_CARDS_IN_PILE = new int[NUMBER_OF_PILES];
        mMSG_CARDS_IN_STACK =  msgCardsInStack;
    }






//    implement methods from parent class RV

    // IMPORTANT
    // onCreateViewHolder does whatever is in this method n times. Where n is the result of getItemCount()
    // 1) inflate new item layout view
    // 2) then it returns this inflated instance

    @NonNull
    @Override
    public CardPilesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_pile_top_card, parent, false);
        return new ViewHolder(itemLayoutView);
    }


    // whenever this is a change then update the outer card which is the next card,
    // AND update the inner card to show either nothing (if pile is empty) or next card that'll be comming
    @Override
    public void onBindViewHolder(@NonNull CardPilesAdapter.ViewHolder holder, int position) {
        updateOuterCard(holder, position); // pass the card that we are up to currently which is a ref to the view object and pass position
        updateInnerCard(holder, position);

    }

    // This will get the value from MainActivity that sets the number of cards in pile.
    private void updateOuterCard(ViewHolder holder, int position) {
        holder.tv_pile_card_cards_in_stack.setText(
                mMSG_CARDS_IN_STACK.concat(Integer.toString(mNUMBER_OF_CARDS_IN_PILE[position]))
        );
    }


    private void updateInnerCard (ViewHolder holder, int position)
    {
        if (mPILE_TOPS[position] != null) {
            populateAndShowInnerCard (holder, position);
        }
        else {
            clearAndHideInnerCard (holder);
        }
    }

    private void populateAndShowInnerCard (ViewHolder holder, int position)
    {
        Card currentCard = mPILE_TOPS[position];

        setTextOfEachTextView (holder, currentCard);
        setColorOfAllTextViews (holder, currentCard);
        holder.cb_pile_card_checkbox.setChecked (mCHECKED_PILES[position]);

        holder.cv_pile_inner_card.setVisibility (View.VISIBLE);
    }

    private void setColorOfAllTextViews (ViewHolder holder, Card currentCard)
    {
        int currentColor = currentCard.getSuit ().getColor ();
        holder.tv_pile_card_rank_top_.setTextColor (currentColor);
        holder.tv_pile_card_suit_center.setTextColor (currentColor);
        holder.tv_pile_card_name_bottom.setTextColor (currentColor);
    }

    private void setTextOfEachTextView (ViewHolder holder, Card currentCard)
    {
        String rankValue = String.format(Locale.getDefault (), "%d", currentCard.getRank ().getValue ());
        holder.tv_pile_card_rank_top_.setText (rankValue);
        holder.tv_pile_card_suit_center.setText (Character.toString (currentCard.getSuit ().getCharacter ()));
        holder.tv_pile_card_name_bottom.setText (currentCard.getRank ().toString ());
    }

    private void clearAndHideInnerCard (ViewHolder holder)
    {
        holder.tv_pile_card_rank_top_.setText("");
        holder.tv_pile_card_suit_center.setText("");
        holder.tv_pile_card_name_bottom.setText("");
        holder.cb_pile_card_checkbox.setChecked (false);

        holder.cv_pile_inner_card.setVisibility (View.INVISIBLE);
    }


    // The adapter will call this method whenever it wants to know how many cards to create.
    @Override
    public int getItemCount() {

        return mPILE_TOPS.length;
    }


    // This will SEND data to the recycler view, changes the DATA SOURCE
    public void updatePile(int pileNumber, Card card, int numberOfCardsInStack) {
        mPILE_TOPS[pileNumber] = card;
        mNUMBER_OF_CARDS_IN_PILE[pileNumber] = numberOfCardsInStack;
        notifyItemChanged(pileNumber);  // this tells the RecyclerView to CHECK for changes.
    }

//    End - implement methods from parent class RV


    // The adapter must clear checkboxes
    public void clearCheck(int position) {
        if(mCHECKED_PILES[position]) {
            mCHECKED_PILES[position] = false;
            notifyItemChanged(position);
        }
    }


    public void toggleCheck(int position) {
        mCHECKED_PILES[position] = !mCHECKED_PILES[position];
        notifyItemChanged(position);
    }

    public boolean[] getCheckedPiles() {
        return mCHECKED_PILES.clone();
    }
    public void overwriteChecksFrom(boolean[] newChecksSet) {
        System.arraycopy(newChecksSet, 0, mCHECKED_PILES, 0, mCHECKED_PILES.length);
    }


    public Card getCardAt(int position) {
        return mPILE_TOPS[position] == null ? null : mPILE_TOPS[position].copy();
    }


    // Use an Animator object to animate the click

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        animateCard(holder.cv_pile_inner_card); // animate the inner card
    }

    private void animateCard(CardView view) {
        // make sure you are using api that supports this animation
        if (Build.VERSION.SDK_INT >= 21) {

            int centerX = 0, centerY = 0, startRadius = 0;
            int endRadius = Math.max(view.getWidth(), view.getHeight());
            Animator circularRevealAnimationOfCard = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius);
            circularRevealAnimationOfCard.start();

        }
    }


    // save state of checked box.



    // this must extend RV.VH so that it can be used in the CardPiles... class
    // needs a constructor
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // This should be an instance of our xml.
        // reference all of our xml files
        public final CardView cv_pile_inner_card;
        public final TextView tv_pile_card_rank_top_, tv_pile_card_name_bottom, tv_pile_card_suit_center, tv_pile_card_cards_in_stack;
        public final CheckBox cb_pile_card_checkbox;


        // Constructor. Call  parent constructor as well.
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // initialize cards
            // note we need itemView since this is an inner class so it has no idea where we are.
            cv_pile_inner_card = itemView.findViewById(R.id.pile_card_inner_card);
            tv_pile_card_rank_top_= itemView.findViewById(R.id.pile_card_rank_top);
            tv_pile_card_name_bottom = itemView.findViewById(R.id.pile_card_name_bottom);
            tv_pile_card_suit_center = itemView.findViewById(R.id.pile_card_suit_center);
            tv_pile_card_cards_in_stack = itemView.findViewById(R.id.pile_card_in_stack);
            cb_pile_card_checkbox = itemView.findViewById(R.id.pile_card_checkbox);

            // This checkbox is not clickable. It is just for design purposes.
            cb_pile_card_checkbox.setClickable(false);


            // We need the set the item view to set onclick listener to  `this` so that whenever we click will call onclick
            itemView.setOnClickListener(this);
        }

        /**
         * Calls the onItemClick method of the implemented (in MainActivity) AdapterOnItemClickListener Interface (below)
         *
         * @param view View that was clicked
         */
        @Override
        public void onClick(View view) {
            sAdapterOnItemClickListener.onItemClick(getAdapterPosition(), view);
        }
    }

    // used to send data out of Adapter - implemented in the calling Activity or Fragment
    @SuppressWarnings("UnusedParameters")
    public interface AdapterOnItemClickListener {
        void onItemClick(int position, View viewClicked);
    }
}



// 1)
// You must provide a viewholder. This is essentially one instance of the xml that will be repeated again and again
// So you must create a class that stores that one instance.
// The instance will have all the checkboxes, text views etc
// then use that instance in our generic adapter
// this class will be inside our main class.
//     public class ViewHolder {
//    }
//


//2) Data that will be fed to the adapter

// 3) Constructor that inits data

// 4) Override the methods


// Implement methods since we are inheriting the abstract RV.Adapter class