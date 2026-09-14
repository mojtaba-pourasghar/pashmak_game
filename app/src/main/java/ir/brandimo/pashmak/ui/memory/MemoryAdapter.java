package ir.brandimo.pashmak.ui.memory;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.brandimo.pashmak.data.catalog.Palette;
import ir.brandimo.pashmak.databinding.ItemMemoryCardBinding;

/** Cards are told apart by color and shape, exactly as the design specified. */
public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.CardHolder> {

    public interface OnCardClick {
        void onCardClick(int position);
    }

    private static final int[] PAIR_COLORS = {
            Palette.RED, Palette.ORANGE, Palette.GREEN,
            Palette.BLUE, Palette.PURPLE, Palette.YELLOW,
            Palette.BROWN, Palette.CHARCOAL
    };
    private static final int SHAPE_CIRCLE = 0;
    private static final int SHAPE_SQUARE = 1;
    private static final int SHAPE_LEAF = 2;

    private final OnCardClick listener;
    private List<Integer> cards = new ArrayList<>();
    private int[] states = new int[0];

    public MemoryAdapter(OnCardClick listener) {
        this.listener = listener;
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
        int face = cards.get(position);
        int state = position < states.length ? states[position] : MemoryViewModel.STATE_FACE_DOWN;
        holder.bind(face, state, position, listener);
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

        void bind(int face, int state, int position, OnCardClick listener) {
            float density = binding.getRoot().getResources().getDisplayMetrics().density;
            boolean revealed = state != MemoryViewModel.STATE_FACE_DOWN;

            GradientDrawable card = new GradientDrawable();
            card.setCornerRadius(18f * density);
            // Every face-down card looks identical, so nothing leaks about the pair.
            card.setColor(revealed ? PAIR_COLORS[face % PAIR_COLORS.length] : 0xFFFFFFFF);
            binding.cardRoot.setBackground(card);
            binding.cardRoot.setAlpha(state == MemoryViewModel.STATE_MATCHED ? 0.45f : 1f);

            GradientDrawable shape = new GradientDrawable();
            shape.setColor(revealed ? 0xF2FFFFFF : 0xFFF3D9D6);
            if (!revealed) {
                shape.setCornerRadius(10f * density);
            } else {
                switch (face % 3) {
                    case SHAPE_CIRCLE:
                        shape.setShape(GradientDrawable.OVAL);
                        break;
                    case SHAPE_SQUARE:
                        shape.setCornerRadius(6f * density);
                        break;
                    case SHAPE_LEAF:
                    default:
                        shape.setCornerRadii(new float[]{
                                24f * density, 24f * density,
                                24f * density, 24f * density,
                                24f * density, 24f * density,
                                0f, 0f});
                        break;
                }
            }
            binding.cardShape.setBackground(shape);

            // A quick half-turn sells the flip without a second layout.
            binding.cardRoot.setRotationY(0f);
            binding.cardRoot.setOnClickListener(v -> {
                v.animate().rotationY(180f).setDuration(160L)
                        .withEndAction(() -> v.setRotationY(0f)).start();
                listener.onCardClick(position);
            });
        }
    }
}
