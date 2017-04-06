package project.hnoct.kitchen.ui;

import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.dialog.AddRecipeDialog;
import project.hnoct.kitchen.dialog.ChapterDetailsDialog;

public class ChapterActivity extends AppCompatActivity implements ChapterDetailsDialog.ChapterDetailsListener, ChapterFragment.RecipeCallBack {
    /** Constants**/
    private static final String LOG_TAG = ChapterActivity.class.getSimpleName();
    private static final String CHAPTER_DETAILS_DIALOG = "chapter_details_dialog";
    private static final String ADD_RECIPE_DIALOG = "add_recipe_dialog";

    /** Member Variables **/
    private long mBookId;

    // Views bound by ButterKnife
    @BindView(R.id.chapter_fab) FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Retrieve URI data to pass to the Fragment
        Uri bookUri = getIntent() != null ? getIntent().getData() : null;

        // Set the member variable bookId
        mBookId = RecipeBookEntry.getRecipeBookIdFromUri(bookUri);

        // Pass the URI to the ChapterFragment as part of a Bundle
        ChapterFragment fragment = new ChapterFragment();
        Bundle args = new Bundle();
        args.putParcelable(ChapterFragment.RECIPE_BOOK_URI, bookUri);
        fragment.setArguments(args);

        // Inflate ChapterFragment into the container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.chapter_container, fragment)
                    .commit();
        }
    }

    @OnClick (R.id.chapter_fab)
    void onFabClick() {
        showAddChapterDialog();
    }

    /**
     * Show the dialog for adding chapters
     */
    void showAddChapterDialog() {
        // Instantiate the dialog
        ChapterDetailsDialog dialog = new ChapterDetailsDialog();
        dialog.show(getFragmentManager(), CHAPTER_DETAILS_DIALOG);

        // Set the listener for the positive click
        dialog.setPositiveClickListener(this);
    }

    @Override
    public void onPositiveDialogClick(DialogFragment dialog, String titleText, String descriptionText) {
        Toast.makeText(this, "Positive clicked!", Toast.LENGTH_SHORT).show();

        // Initialize parameters for inserting chapter into database
        Uri chapterUri = ChapterEntry.CONTENT_URI;
        String selection = RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
        String[] selectionArgs = new String[] {Long.toString(mBookId)};

        // Determine the chapter order
        Cursor cursor = getContentResolver().query(
                chapterUri,
                new String[] {ChapterEntry.COLUMN_CHAPTER_ORDER},
                selection,
                selectionArgs,
                ChapterEntry.COLUMN_CHAPTER_ORDER + " DESC"
        );

        int chapterOrder;
        if (cursor != null && cursor.moveToFirst()) {
            chapterOrder = cursor.getInt(0) + 1;
        } else {
            chapterOrder = 0;
        }

        // Create ContentValues to insert
        ContentValues values = new ContentValues();
        values.put(ChapterEntry.COLUMN_CHAPTER_NAME, titleText);
        values.put(ChapterEntry.COLUMN_CHAPTER_DESCRIPTION, descriptionText);
        values.put(ChapterEntry.COLUMN_CHAPTER_ORDER, chapterOrder);
        values.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_ID, mBookId);

        // Insert chapter into database
        getContentResolver().insert(
                chapterUri,
                values
        );
    }

    @Override
    public void onRecipeSelected(String recipeUrl, RecipeAdapter.RecipeViewHolder viewHolder) {
        // Start the RecipeDetailsActivity for the selected recipe
        Intent intent = new Intent(this, RecipeDetailsActivity.class);
        intent.setData(Uri.parse(recipeUrl));
        startActivity(intent);
    }

    @Override
    public void onAddRecipeClicked(final long chapterId) {
        // Initialize the Dialog that will show recipes
        AddRecipeDialog dialog = new AddRecipeDialog();

        // Set the listener for when a recipe is selected
        dialog.setSelectionListener(new AddRecipeDialog.SelectionListener() {

            @Override
            public void onRecipeSelected(long recipeId) {
                // Initialize parameters for querying the database for recipe order
                Uri linkRecipeBookUri = LinkRecipeBookTable.CONTENT_URI;
                String[] projection = LinkRecipeBookTable.PROJECTION;
                String selection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ?";
                String[] selectionArgs = new String[] {Long.toString(chapterId)};
                String sortOrder = LinkRecipeBookTable.COLUMN_RECIPE_ORDER + " DESC";

                // Query the database to determine the new recipe's order in the chapter
                Cursor cursor = getContentResolver().query(
                        linkRecipeBookUri,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                );

                int recipeOrder;
                if (cursor !=  null && cursor.moveToFirst()) {
                    recipeOrder = cursor.getInt(LinkRecipeBookTable.IDX_RECIPE_ORDER) + 1;

                    // Close the Cursor
                    cursor.close();
                } else {
                    recipeOrder = 0;
                }

                // Create the ContentValues to insert into the linked recipe book table
                ContentValues values = new ContentValues();
                values.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_ID, mBookId);
                values.put(ChapterEntry.COLUMN_CHAPTER_ID, chapterId);
                values.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                values.put(LinkRecipeBookTable.COLUMN_RECIPE_ORDER, recipeOrder);

                // Insert values into database
                getContentResolver().insert(
                        linkRecipeBookUri,
                        values
                );
            }
        });

        // Show the Dialog
        dialog.show(getFragmentManager(), ADD_RECIPE_DIALOG);
    }
}
