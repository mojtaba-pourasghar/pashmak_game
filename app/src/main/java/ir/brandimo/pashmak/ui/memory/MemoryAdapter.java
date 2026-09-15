package ir.brandimo.pashmak.ui.memory;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.data.catalog.MemoryDeck;
import ir.brandimo.pashmak.data.catalog.Palette;
import ir.brandimo.pashmak.databinding.ItemMemoryCardBinding;

/** Draws the board. A face-down card is always the same, whatever it hides. */
public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.CardHolder> {

    public interface OnCardClick {
        void onCardClick(int position);
    }

    private final OnCardClick listener;
    private List<Integer> cards = new ArrayList<>();
    private int[] states = new int[0];
    private MemoryDeck deck;
    private int cardHeightPx;

    public MemoryAdapter(OnCardClick listener) {
        this.listener = listener;
    }

    public void setDeck(MemoryDeck deck) {
        this.deck = deck;
    }

    /** The board is sized to fit without scrolling, so rows get an exact height. */
    public void setCardHeight(int heightPx) {
        cardHeightPx = heightPx;
    }

    public void setCards(List<Integer> next) {
        cards = next == null ? new ArrayList<>() : next;
        notifyDataSetChanged();
    }

    public void setStates(int[] next) {
        states = next == null ? new int[0] : next;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardHolder(ItemMemoryCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CardHolder holder, int position) {
        int state = position < states.length
                ? states[position] : MemoryViewModel.STATE_FACE_DOWN;
        holder.bind(deck, cards.get(position), state, position, cardHeightPx, listener);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class CardHolder extends RecyclerView.ViewHolder {

        private final ItemMemoryCardBinding binding;

        CardHolder(ItemMemoryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MemoryDeck deck, int faceIndex, int state, int position,
                  int heightPx, OnCardClick listener) {
            if (heightPx > 0) {
                ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
                if (params != null && params.height != heightPx) {
                    params.height = heightPx;
                    binding.getRoot().setLayoutParams(params);
                }
            }

            boolean revealed = state != MemoryViewModel.STATE_FACE_DOWN;
            MemoryDeck.Face face = deck == null ? null
                    : deck.faces.get(faceIndex % deck.size());

            float density = binding.getRoot().getResources().getDisplayMetrics().density;
            GradientDrawable card = new GradientDrawable();
            card.setCornerRadius(18f * density);
            if (revealed && face != null) {
                // Pale ground, full-strength rim: the card still reads as "the red
                // one" without swallowing artwork drawn in that same red.
                card.setColor(Palette.pale(face.tint));
                card.setStroke(Math.round(2f * density), face.tint);
            } else {
                card.setColor(0xFFFFFFFF);
            }
            binding.cardRoot.setBackground(card);
            binding.cardRoot.setAlpha(state == MemoryViewModel.STATE_MATCHED ? 0.5f : 1f);

            binding.cardBack.setVisibility(revealed ? View.GONE : View.VISIBLE);
            boolean glyph = revealed && face != null && face.isGlyph();
            boolean picture = revealed && face != null && !glyph;
            binding.cardIcon.setVisibility(picture ? View.VISIBLE : View.GONE);
            binding.cardGlyph.setVisibility(glyph ? View.VISIBLE : View.GONE);

            // Always clear first: a recycled holder can still be finishing a load for
            // whichever card it showed last, which would flash the wrong drawing.
            Glide.with(binding.cardIcon).clear(binding.cardIcon);
            if (glyph) {
                binding.cardGlyph.setText(face.glyph);
                binding.cardGlyph.setTextColor(face.tint);
            } else if (picture && face.isPhoto()) {
                Glide.with(binding.cardIcon)
                        .load(new File(face.path))
                        .fitCenter()
                        .into(binding.cardIcon);
            } else if (picture) {
                binding.cardIcon.setImageResource(face.icon);
            } else {
                binding.cardIcon.setImageDrawable(null);
            }

            binding.cardRoot.setOnClickListener(v -> {
                if (state == MemoryViewModel.STATE_FACE_DOWN) {
                    v.animate().rotationY(180f).setDuration(150L)
                            .withEndAction(() -> v.setRotationY(0f)).start();
                }
                listener.onCardClick(position);
            });
        }
    }
}
