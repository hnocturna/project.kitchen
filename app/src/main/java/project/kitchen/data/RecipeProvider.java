package project.kitchen.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import project.kitchen.data.RecipeContract.*;


/**
 * Created by hnoct on 2/16/2017.
 * ContentProvider with methods for accessing, inserting, modifying, and deleting from the database
 */

public class RecipeProvider extends ContentProvider {
    /** Constants **/
    // Return values for the {@link #sUriMatcher}
    private static final int RECIPE = 100;
    private static final int RECIPE_WITH_ID = 101;
    private static final int RECIPE_AND_INGREDIENT = 200;
    private static final int RECIPE_AND_INGREDIENT_QUERY = 201;
    private static final int INGREDIENT = 300;
    private static final int INGREDIENT_WITH_ID = 301;
    private static final int INGREDIENT_SEARCH = 302;
    private static final int RECIPE_BOOK = 400;
    private static final int RECIPE_BOOK_WITH_ID = 401;
    private static final int CHAPTER = 500;
    private static final int CHAPTER_WITH_ID = 501;
    private static final int RECIPE_BOOK_LINK = 600;
    private static final int RECIPE_BOOK_LINK_QUERY_CHAPTERS = 601;
    private static final int RECIPE_BOOK_LINK_QUERY_RECIPES = 602;
    private static final int RECIPE_BOOK_LINK_QUERY_RECIPES_WITH_CHAPTER = 603;

    private static final SQLiteQueryBuilder sRecipeAndIngredientQueryBuilder;
    static {
        // Joins the three tables together so that they can be queried as a single entity
        sRecipeAndIngredientQueryBuilder = new SQLiteQueryBuilder();
        sRecipeAndIngredientQueryBuilder.setTables(
                RecipeEntry.TABLE_NAME + " INNER JOIN " +
                        LinkIngredientEntry.TABLE_NAME + " ON " +
                        RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = " +
                        LinkIngredientEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " INNER JOIN " +
                        IngredientEntry.TABLE_NAME + " ON " +
                        LinkIngredientEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID + " = " +
                        IngredientEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID
        );
    }

    private static final SQLiteQueryBuilder sRecipeBookChapterAndRecipeQueryBuilder;
    static {
        // Joins recipe_books, chapters, recipes, and recipe_book_link tables together to be queried as a
        // single entity
        sRecipeBookChapterAndRecipeQueryBuilder = new SQLiteQueryBuilder();
        sRecipeBookChapterAndRecipeQueryBuilder.setTables(
                RecipeEntry.TABLE_NAME + " INNER JOIN " +
                        LinkRecipeBookEntry.TABLE_NAME + " ON " +
                        RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = " +
                        LinkRecipeBookEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " INNER JOIN " +
                        RecipeBookEntry.TABLE_NAME + " ON " +
                        LinkRecipeBookEntry.TABLE_NAME + "." + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = " +
                        RecipeBookEntry.TABLE_NAME + "." + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " INNER JOIN " +
                        ChapterEntry.TABLE_NAME + " ON " +
                        LinkRecipeBookEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = " +
                        ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID
        );
    }

    // UriMatcher used by this Content Provider for differentiating the data being accessed based
    // on a provided URI
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    /** Member Variables **/
    private RecipeDbHelper mDbHelper;

    /**
     * Matches URIs to the content provider can access the correct database and rows
     * @return UriMatcher that will match the query to the operation
     */
    private static UriMatcher buildUriMatcher() {
        /** Constants **/
        final String authority = RecipeContract.CONTENT_AUTHORITY;

        // Root URI
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // URIs to be matched
        uriMatcher.addURI(authority, RecipeContract.PATH_RECIPE, RECIPE);
        uriMatcher.addURI(authority, RecipeContract.PATH_RECIPE + "/#", RECIPE_WITH_ID);

        uriMatcher.addURI(authority, RecipeContract.PATH_INGREDIENT, INGREDIENT);
        uriMatcher.addURI(authority, RecipeContract.PATH_INGREDIENT + "/#", INGREDIENT_WITH_ID);
        uriMatcher.addURI(authority, RecipeContract.PATH_INGREDIENT + "/*", INGREDIENT_SEARCH);

        uriMatcher.addURI(authority, RecipeContract.PATH_INGREDIENT_LINK, RECIPE_AND_INGREDIENT);      // Used for inserting into Link Table
        uriMatcher.addURI(authority, RecipeContract.PATH_INGREDIENT_LINK + "/" + RecipeEntry.TABLE_NAME + "/*", RECIPE_AND_INGREDIENT_QUERY);
        uriMatcher.addURI(authority, RecipeContract.PATH_INGREDIENT_LINK + "/" + IngredientEntry.TABLE_NAME +  "/#", RECIPE_AND_INGREDIENT_QUERY);

        uriMatcher.addURI(authority, RecipeContract.PATH_BOOK, RECIPE_BOOK);
        uriMatcher.addURI(authority, RecipeContract.PATH_BOOK + "/#", RECIPE_BOOK_WITH_ID);

        uriMatcher.addURI(authority, RecipeContract.PATH_CHAPTER, CHAPTER);
        uriMatcher.addURI(authority, RecipeContract.PATH_CHAPTER + "/#", CHAPTER_WITH_ID);

        uriMatcher.addURI(authority, RecipeContract.PATH_BOOK_LINK, RECIPE_BOOK_LINK);
        uriMatcher.addURI(authority, RecipeContract.PATH_BOOK_LINK + "/#/" + ChapterEntry.TABLE_NAME, RECIPE_BOOK_LINK_QUERY_CHAPTERS);
        uriMatcher.addURI(authority, RecipeContract.PATH_BOOK_LINK + "/#/" + RecipeEntry.TABLE_NAME, RECIPE_BOOK_LINK_QUERY_RECIPES);
        uriMatcher.addURI(authority, RecipeContract.PATH_BOOK_LINK + "/#/" + ChapterEntry.TABLE_NAME + "/#", RECIPE_BOOK_LINK_QUERY_RECIPES_WITH_CHAPTER);

        return uriMatcher;
    }

    // Static constants for querying with selection
    private static final String sRecipeSelection = RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = ?";
    private static final String sIngredientSelection = IngredientEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID + " = ?";
    private static final String sRecipeBookSelection = RecipeBookEntry.TABLE_NAME + "." + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
    private static final String sChapterSelection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ?";

    /**
     * Helper method for creating a {@link Cursor} given a URI containing a recipeId
     * @param uri the Uri containing the recipeId
     * @param projection columns to return
     * @param sortOrder sort order
     * @return {@link Cursor} selecting the recipe in the database
     */
    private Cursor getRecipeById(Uri uri, String[] projection, String sortOrder) {
        // Retrieve the recipeId from the provided URI
        long recipeId = RecipeEntry.getRecipeIdFromUri(uri);

        // Set the selection and its args equal to the recipeId column and the recipeId respectively
        String selection = sRecipeSelection;
        String[] selectionArgs = new String[] {Long.toString(recipeId)};

        // Create and return the cursor
        return mDbHelper.getReadableDatabase().query(
                RecipeEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /** @see #getRecipeById(Uri, String[], String) **/
    private Cursor getIngredientById(Uri uri, String[] projection, String sortOrder) {
        long ingredientId = IngredientEntry.getIngredientIdFromUri(uri);

        String selection = sIngredientSelection;
        String[] selectionArgs = new String[] {Long.toString(ingredientId)};

        return mDbHelper.getReadableDatabase().query(
                IngredientEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /** @see #getRecipeById(Uri, String[], String) **/
    private Cursor getRecipeBookById(Uri uri, String[] projection, String sortOrder) {
        long recipeBookId = RecipeBookEntry.getRecipeBookIdFromUri(uri);

        String selection = sRecipeBookSelection;
        String[] selectionArgs = new String[] {Long.toString(recipeBookId)};

        return mDbHelper.getReadableDatabase().query(
                RecipeBookEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /** @see #getRecipeById(Uri, String[], String) **/
    private Cursor getChapterById(Uri uri, String[] projection, String sortOrder) {
        long chapterId = ChapterEntry.getChapterIdFromUri(uri);

        String selection = sChapterSelection;
        String[] selectionArgs = new String[] {Long.toString(chapterId)};

        return mDbHelper.getReadableDatabase().query(
                ChapterEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /**
     * Helper method for building a query for all ingredients for a given recipe or vice-versa
     * @param uri Uri containing either a query for a recipe or an ingredient
     * @param projection columns to return
     * @param sortOrder order to sort in
     * @return Cursor selecting all returned items of a given query (e.g. all ingredients for a recipe)
     */
    private Cursor filterRecipeAndIngredientById(Uri uri, String[] projection, String sortOrder) {
        // Attempt to retrieve both a recipeId and ingredientId from the URI
        long recipeId = LinkIngredientEntry.getRecipeIdFromUri(uri);
        long ingredientId = LinkIngredientEntry.getIngredientIdFromUri(uri);

        if (recipeId != -1) {
            // If recipeId can be retrieved, then query all ingredients for a given recipe

            // Select for the ingredientId column
            String selection = sRecipeSelection;

            // Filter for ingredients that match recipeId
            String[] selectionArgs = new String[] {Long.toString(recipeId)};

            // Return the Cursor
            return sRecipeAndIngredientQueryBuilder.query(
                    mDbHelper.getReadableDatabase(),
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
        } else {
            // If recipeId cannot be retrieved, then ingredientId is being queried

            // Select for the ingredientsId column
            String selection = sIngredientSelection;

            // Filter by recipes that contain an ingredient
            String[] selectionArgs = new String[] {Long.toString(ingredientId)};

            // Return the Cursor
            return sRecipeAndIngredientQueryBuilder.query(
                    mDbHelper.getReadableDatabase(),
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
        }
    }

    /** @see #getRecipeById(Uri, String[], String) **/
    private Cursor getChaptersFromRecipeBook(Uri uri, String[] projection, String sortOrder) {
        long recipeBookId = LinkRecipeBookEntry.getRecipeBookIdFromUri(uri);

        String selection = sRecipeBookSelection;
        String[] selectionArgs = new String[] {Long.toString(recipeBookId)};

        return sRecipeBookChapterAndRecipeQueryBuilder.query(
                mDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /** @see #getRecipeById(Uri, String[], String) **/
    private Cursor getRecipesFromRecipeBook(Uri uri, String[] projection, String sortOrder) {
        long recipeBookId = LinkRecipeBookEntry.getRecipeBookIdFromUri(uri);

        String selection = sRecipeBookSelection;
        String[] selectionArgs = new String[] {Long.toString(recipeBookId)};

        return sRecipeBookChapterAndRecipeQueryBuilder.query(
                mDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /** @see #getRecipeById(Uri, String[], String) **/
    private Cursor getRecipesFromRecipeBookWithChapter(Uri uri, String[] projection, String sortOrder) {
        long recipeBookId = LinkRecipeBookEntry.getRecipeBookIdFromUri(uri);
        long chapterId = LinkRecipeBookEntry.getChapterIdFromUri(uri);

        String selection = sRecipeBookSelection + " AND " + sChapterSelection;
        String[] selectionArgs = new String[] {Long.toString(recipeBookId), Long.toString(chapterId)};

        return sRecipeBookChapterAndRecipeQueryBuilder.query(
                mDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new RecipeDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RECIPE:                            // For accessing Recipe Table
                return RecipeEntry.CONTENT_TYPE;
            case RECIPE_WITH_ID:                    // For accessing a single row on the Recipe Table
                return RecipeEntry.CONTENT_ITEM_TYPE;
            case INGREDIENT:                        // For accessing the Ingredient Table
                return IngredientEntry.CONTENT_TYPE;
            case INGREDIENT_WITH_ID:                // For accessing a single row on the Ingredient Table
                return IngredientEntry.CONTENT_ITEM_TYPE;
            case RECIPE_AND_INGREDIENT:             // For accessing the linked tables
                return LinkIngredientEntry.CONTENT_TYPE;
            case RECIPE_AND_INGREDIENT_QUERY:       // For filtering the linked table for one or the other
                return LinkIngredientEntry.CONTENT_TYPE;
            case RECIPE_BOOK:                       // For accessing the Recipe Book Table
                return RecipeBookEntry.CONTENT_TYPE;
            case RECIPE_BOOK_WITH_ID:               // For accessing a single row on the Recipe Book Table
                return RecipeBookEntry.CONTENT_ITEM_TYPE;
            case CHAPTER:                           // For accessing the Chapter Table
                return ChapterEntry.CONTENT_TYPE;
            case CHAPTER_WITH_ID:                   // For accessing a single row in the Chapter Table
                return ChapterEntry.CONTENT_ITEM_TYPE;
            case RECIPE_BOOK_LINK:                  // For accessing the linked tables
                return RecipeBookEntry.CONTENT_TYPE;
            case RECIPE_BOOK_LINK_QUERY_CHAPTERS:            // For filtering the linked tables with a query
                return RecipeBookEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Cursor to be returned by the query
        Cursor cursor;

        switch (sUriMatcher.match(uri)) {
            case RECIPE: {
                // Querying recipe table
                cursor = mDbHelper.getReadableDatabase().query(
                        RecipeEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case RECIPE_WITH_ID: {
                // Filter query for a single recipe
                cursor = getRecipeById(uri, projection, sortOrder);
                break;
            }
            case INGREDIENT: {
                // Querying ingredient table
                cursor = mDbHelper.getReadableDatabase().query(
                        IngredientEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case INGREDIENT_WITH_ID: {
                // Filter query by a single ingredient
                cursor = getIngredientById(uri, projection, sortOrder);
                break;
            }
            case INGREDIENT_SEARCH: {

            }
            case RECIPE_AND_INGREDIENT: {
                cursor = sRecipeAndIngredientQueryBuilder.query(
                        mDbHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case RECIPE_AND_INGREDIENT_QUERY: {
                // Query all tables and filter by specific recipe or ingredient
                cursor = filterRecipeAndIngredientById(uri, projection, sortOrder);
                break;
            }
            case RECIPE_BOOK: {
                cursor = mDbHelper.getReadableDatabase().query(
                        RecipeBookEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case RECIPE_BOOK_WITH_ID: {
                cursor = getRecipeBookById(uri, projection, sortOrder);
                break;
            }
            case CHAPTER: {
                cursor = mDbHelper.getReadableDatabase().query(
                        ChapterEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case CHAPTER_WITH_ID: {
                cursor =  getChapterById(uri, projection, sortOrder);
                break;
            }
            case RECIPE_BOOK_LINK: {
                cursor = sRecipeBookChapterAndRecipeQueryBuilder.query(
                        mDbHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case RECIPE_BOOK_LINK_QUERY_CHAPTERS: {
                cursor = getChaptersFromRecipeBook(uri, projection, sortOrder);
                break;
            }
            case RECIPE_BOOK_LINK_QUERY_RECIPES: {
                cursor = getRecipesFromRecipeBook(uri, projection, sortOrder);
                break;
            }
            case RECIPE_BOOK_LINK_QUERY_RECIPES_WITH_CHAPTER: {
                cursor = getRecipesFromRecipeBookWithChapter(uri, projection, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        // Notify listeners of change in data associated with the URI
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        // Uri pointing to the row inserted into the database
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case RECIPE: {
                // Inserting a recipe
                long _id = mDbHelper.getWritableDatabase().insert(
                        RecipeEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = RecipeEntry.buildRecipeUri(_id);
                } else {
                    throw new SQLException("Error inserting recipe into database.");
                }
                break;
            }
            case INGREDIENT: {
                // Inserting an ingredient
                long _id = mDbHelper.getWritableDatabase().insert(
                        IngredientEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = IngredientEntry.buildIngredientUri(_id);
                } else {
                    throw new SQLException("Error inserting ingredient into database.");
                }
                break;
            }
            case RECIPE_AND_INGREDIENT: {
                // Inserting recipe, ingredient, and quantity into link table
                long _id = mDbHelper.getWritableDatabase().insert(
                        LinkIngredientEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = LinkIngredientEntry.buildIngredientLinkUri(_id);
                } else {
                    throw new SQLException("Error inserting quantity and ingredient order into ingredient link table.");
                }
                break;
            }
            case RECIPE_BOOK: {
                // Inserting a recipe book
                long _id = mDbHelper.getWritableDatabase().insert(
                        RecipeBookEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = RecipeBookEntry.buildRecipeBookUri(_id);
                } else {
                    throw new SQLException("Error inserting recipe book into database.");
                }
                break;
            }
            case CHAPTER: {
                // Inserting a chapter
                long _id = mDbHelper.getWritableDatabase().insert(
                        ChapterEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = ChapterEntry.buildChapterUri(_id);
                } else {
                    throw new SQLException("Error inserting chapter into database.");
                }
                break;
            }
            case RECIPE_BOOK_LINK: {
                // Inserting recipe order into the recipe_book_link table
                long _id = mDbHelper.getWritableDatabase().insert(
                        LinkRecipeBookEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = LinkRecipeBookEntry.buildRecipeBookLinkUri(_id);
                } else {
                    throw new SQLException("Error inserting recipe order into recipe book link table.");
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        // Notify listeners of change in data associated with the URI
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        /** Constants **/
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Return the number of rows deleted
        int rowsDeleted;

        switch (sUriMatcher.match(uri)) {
            case RECIPE: {
                // Delete from recipe table
                rowsDeleted = db.delete(
                        RecipeEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            case INGREDIENT: {
                // Delete from Ingredient Table
                rowsDeleted = db.delete(
                        IngredientEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            case RECIPE_AND_INGREDIENT: {
                // Delete from Link Table
                rowsDeleted = db.delete(
                        LinkIngredientEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            case RECIPE_BOOK: {
                // Delete from Recipe Book Table
                rowsDeleted = db.delete(
                        RecipeBookEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            case CHAPTER: {
                // Delete from Chapter Table
                rowsDeleted = db.delete(
                        ChapterEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            case RECIPE_BOOK_LINK: {
                // Delete from Recipe Book Link Table
                rowsDeleted = db.delete(
                        LinkRecipeBookEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            }
            default: throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        // Notify listeners of change in data associated with the URI
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        /** Constants **/
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Number of rows updated by the operation
        int rowsUpdated;

        switch (sUriMatcher.match(uri)) {
            case RECIPE: {
                // Updating rows in the recipe table
                 rowsUpdated = db.update(
                         RecipeEntry.TABLE_NAME,
                         contentValues,
                         selection,
                         selectionArgs
                 );
                break;
            }
            case INGREDIENT: {
                // Updating rows in the ingredient table
                rowsUpdated = db.update(
                        IngredientEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            }
            case RECIPE_AND_INGREDIENT: {
                // Updating rows in the link table
                rowsUpdated = db.update(
                        LinkIngredientEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            }
            case RECIPE_BOOK: {
                // Updating rows in the recipe book table
                rowsUpdated = db.update(
                        RecipeBookEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            }
            case CHAPTER: {
                // Updating rows in the chapter table
                rowsUpdated = db.update(
                        ChapterEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            }
            case RECIPE_BOOK_LINK: {
                // Updating rows in the recipe book link table
                rowsUpdated = db.update(
                        LinkRecipeBookEntry.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;
            }
            default: throw new UnsupportedOperationException("Unknown URI:" + uri);
        }
        // Notify listeners of change in data associated with the URI
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private ArrayList<String> searchIngredient(String searchTerm) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        if (searchTerm != null &&  searchTerm.length() > 2) {
            ArrayList<String> searchResults = new ArrayList<>();

            Cursor cursor = db.query(
                    IngredientEntry.TABLE_NAME,
                    new String[] {IngredientEntry.COLUMN_INGREDIENT_ID, IngredientEntry.COLUMN_INGREDIENT_NAME},
                    IngredientEntry.COLUMN_INGREDIENT_NAME + " LIKE ?",
                    new String[] {"%" + searchTerm + "%"},
                    null,
                    null,
                    IngredientEntry.COLUMN_INGREDIENT_ID + " ASC",
                    "0,5"
            );

            if (cursor.moveToFirst()) {
                do {
                    searchResults.add(cursor.getString(cursor.getColumnIndex(IngredientEntry.COLUMN_INGREDIENT_NAME)));
                } while (cursor.moveToNext());
            }

            cursor.close();

            return searchResults;
        }
        return null;
    }

    private ArrayList<String> searchRecipeFavorites(String searchTerm) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        if (searchTerm != null &&  searchTerm.length() > 2) {
            ArrayList<String> searchResults = new ArrayList<>();

            Cursor cursor = db.query(
                    RecipeEntry.TABLE_NAME,
                    new String[] {RecipeEntry.COLUMN_RECIPE_NAME},
                    RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?",
                    new String[] {"%" + searchTerm + "%"},
                    null,
                    null,
                    RecipeEntry.COLUMN_RECIPE_NAME + " ASC",
                    "0,5"
            );

            if (cursor.moveToFirst()) {
                do {
                    searchResults.add(cursor.getString(cursor.getColumnIndex(RecipeEntry.COLUMN_RECIPE_NAME)));
                } while (cursor.moveToNext());
            }

            cursor.close();

            return searchResults;
        }
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        /** Constants **/
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Number of rows inserted
        int rowsInserted = 0;

        switch(sUriMatcher.match(uri)) {
            case RECIPE: {
                // Insert multiple recipes
                // Prepare database for multiple operations
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        // Insert each ContentValues into the database
                        long _id = db.insert(
                                RecipeEntry.TABLE_NAME,
                                null,
                                value
                        );
                        if (_id > 0) {
                            rowsInserted++;
                        }
                    }
                    // End instructions for this operation
                    db.setTransactionSuccessful();
                } finally {
                    // Commit operations to the database
                    db.endTransaction();
                }
                // Notifies content observers that the data at the URI has been updated. Ensures
                // references to the data are always up-to-date
                getContext().getContentResolver().notifyChange(uri, null);
                return rowsInserted;
            }
            case INGREDIENT: {
                // Insert multiple ingredients
                /** See above comments for details **/
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(
                                IngredientEntry.TABLE_NAME,
                                null,
                                value
                        );
                        if (_id > 0) {
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return rowsInserted;
            }
            case RECIPE_AND_INGREDIENT: {
                // Insert multiple rows into the link table
                /** See above comments for details **/
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(
                                LinkIngredientEntry.TABLE_NAME,
                                null,
                                value
                        );
                        if (_id > 0) {
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return rowsInserted;
            }
            case RECIPE_BOOK: {
                // Insert multiple rows into the recipe book table
                /** See above comments for details **/
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(
                                RecipeBookEntry.TABLE_NAME,
                                null,
                                value
                        );
                        if (_id > 0) {
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return rowsInserted;
            }
            case CHAPTER: {
                // Insert multiple rows into the chapter table
                /** See above comments for details **/
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(
                                ChapterEntry.TABLE_NAME,
                                null,
                                value
                        );
                        if (_id > 0) {
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return rowsInserted;
            }
            case RECIPE_BOOK_LINK: {
                // Insert multiple rows into the link table
                /** See above comments for details **/
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(
                                LinkRecipeBookEntry.TABLE_NAME,
                                null,
                                value
                        );
                        if (_id > 0) {
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return rowsInserted;
            }
            default: return super.bulkInsert(uri, values);
        }
    }

    @Nullable
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method.equals(RecipeContract.SEARCH_INGREDIENT)) {
            Bundle bundle = new Bundle();
            ArrayList<String> searchResults = searchIngredient(arg);
            bundle.putStringArrayList(RecipeContract.INGREDIENT_SEARCH_KEY, searchResults);
            return bundle;
        }
        if (method.equals(RecipeContract.SEARCH_FAVORITES)) {
            Bundle bundle = new Bundle();
            ArrayList<String> searchResults = searchRecipeFavorites(arg);
            bundle.putStringArrayList(RecipeContract.RECIPE_FAVORITES_SEARCH_KEY, searchResults);
            return bundle;
        }
        return null;
    }
}
