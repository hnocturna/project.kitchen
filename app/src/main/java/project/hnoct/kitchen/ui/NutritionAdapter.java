package project.hnoct.kitchen.ui;

import android.content.Context;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;


/**
 * Created by hnoct on 2/26/2017.
 */

public class NutritionAdapter extends RecyclerView.Adapter<NutritionAdapter.NutritionViewHolder> {
    /** Constants **/
    private static final String LOG_TAG = NutritionAdapter.class.getSimpleName();

    /** Member Variables **/
    private static Context mContext;
    private static List<Pair<Integer, Double>> mNutritionList;
    private int mNutritionBarWidthTotal;

    public NutritionAdapter(Context context) {
        mContext = context;
    }
    @Override
    public NutritionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_nutrient, parent, false);
        view.setFocusable(false);
        return new NutritionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NutritionViewHolder holder, int position) {
        if (mNutritionBarWidthTotal == 0) {
            mNutritionBarWidthTotal = holder.nutrientPercentageBackground.getWidth();
        }

        @RecipeEntry.NutrientType int nutrientType = mNutritionList.get(position).first;
        double nutrientValue = mNutritionList.get(position).second;
        double nutrientPercentage = Utilities.getDailyValues(mContext, nutrientType, nutrientValue);
        String nutrientValueString = Utilities.formatNutrient(mContext, nutrientType, nutrientValue);

        DecimalFormat df = new DecimalFormat("#0%");
        String nutrientPercentageString = df.format(nutrientPercentage);
        int percentageBarWidth = (int) (mNutritionBarWidthTotal * nutrientPercentage);

        holder.nutrientText.setText(Utilities.getNutrientType(mContext, nutrientType));
        holder.nutrientValueText.setText(nutrientValueString);
        holder.nutrientPercentageText.setText(nutrientPercentageString);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(percentageBarWidth, holder.nutrientPercentageBar.getHeight());
        holder.nutrientPercentageBar.setMinimumWidth(percentageBarWidth);
        holder.nutrientPercentageBar.requestLayout();
    }

    @Override
    public int getItemCount() {
        if (mNutritionList != null) {
            return mNutritionList.size();
        }
        return 0;
    }

    public void setNutritionList(List<Pair<Integer, Double>> nutritionList) {
        mNutritionList = nutritionList;
        notifyDataSetChanged();
    }

    public class NutritionViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_nutrient_text) TextView nutrientText;
        @BindView(R.id.list_nutrient_value_text) TextView nutrientValueText;
        @BindView(R.id.list_nutrient_percentage_text) TextView nutrientPercentageText;
        @BindView(R.id.list_nutrient_percentage_bar) ImageView nutrientPercentageBar;
        @BindView(R.id.list_nutrient_percentage_background) FrameLayout nutrientPercentageBackground;

        public NutritionViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}