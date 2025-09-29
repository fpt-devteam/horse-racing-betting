package com.example.horse_racing_betting.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.model.Bet;

import java.util.ArrayList;
import java.util.List;

public class BetAdapter extends RecyclerView.Adapter<BetAdapter.BetViewHolder> {
    private List<Bet> bets = new ArrayList<>();
    private OnBetClickListener listener;

    public interface OnBetClickListener {
        void onRemoveBet(int position);
    }

    public BetAdapter(OnBetClickListener listener) {
        this.listener = listener;
    }

    public void updateBets(List<Bet> newBets) {
        this.bets = newBets != null ? new ArrayList<>(newBets) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bet, parent, false);
        return new BetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BetViewHolder holder, int position) {
        Bet bet = bets.get(position);
        holder.bind(bet, position);
    }

    @Override
    public int getItemCount() {
        return bets.size();
    }

    class BetViewHolder extends RecyclerView.ViewHolder {
        private TextView tvBetDetails;
        private ImageButton btnRemoveBet;

        public BetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBetDetails = itemView.findViewById(R.id.tvBetDetails);
            btnRemoveBet = itemView.findViewById(R.id.btnRemoveBet);
        }

        public void bind(Bet bet, int position) {
            tvBetDetails.setText(bet.toString());
            btnRemoveBet.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveBet(position);
                }
            });
        }
    }
}