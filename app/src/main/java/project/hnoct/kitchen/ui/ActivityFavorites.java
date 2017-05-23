package project.hnoct.kitchen.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeDbHelper;
import project.hnoct.kitchen.prefs.SettingsActivity;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;

public class ActivityFavorites extends AppCompatActivity implements FragmentFavorites.RecipeCallBack {
    // Member Variables
    private ActionBarDrawerToggle mDrawerToggle;

    // Views Bound by ButterKnife
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.main_drawer_layout) DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        // Set up the hamburger menu used for opening mDrawerLayout
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.button_confirm, R.string.button_deny);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectDrawerItem(item);
                return true;
            }
        });
    }

    private void selectDrawerItem(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_browse: {
                Intent intent = new Intent(this, ActivityRecipeList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                hideNavigationDrawer();
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_favorites: {
                break;
            }
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                hideNavigationDrawer();
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_my_recipes: {
                startActivity(new Intent(this, ActivityMyRecipes.class));
                hideNavigationDrawer();
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_my_recipe_books: {
                hideNavigationDrawer();
                Intent intent = new Intent(this, ActivityRecipeBook.class);
                startActivity(intent);
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_shopping_list: {
                hideNavigationDrawer();
                Intent intent = new Intent(this, ActivityShoppingList.class);
                startActivity(intent);
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
        }
    }

    /**
     * Hides the Navigation Drawer
     */
    private void hideNavigationDrawer() {
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    @Override
    public void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        // If in single-view mode, then start the ActivityRecipeDetails
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                new Pair(viewHolder.recipeImage, getString(R.string.transition_recipe_image))
        );
        Intent intent = new Intent(this, ActivityRecipeDetails.class);
        intent.setData(Uri.parse(recipeUrl));
        intent.putExtra(getString(R.string.extra_image), imageUrl);
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }
}
