package project.hnoct.kitchen.ui;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import project.hnoct.kitchen.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class ActivitySearchFragment extends Fragment {

    public ActivitySearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_search, container, false);
    }
}
