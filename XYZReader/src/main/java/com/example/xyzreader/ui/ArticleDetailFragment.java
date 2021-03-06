package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    private static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private FragmentActivity mFragmentActivity;


    private ImageView mPhotoView;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentActivity = getActivityCast();

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        setHasOptionsMenu(true);
    }

    private ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = mRootView.findViewById(R.id.photo);
        Toolbar mToolbar = mRootView.findViewById(R.id.toolbar);

        if (mToolbar != null) {
            getActivityCast().setSupportActionBar(mToolbar);
            if (getActivityCast().getSupportActionBar() != null) {
                getActivityCast().getSupportActionBar().setDisplayShowTitleEnabled(false);
                getActivityCast().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFragmentActivity.onBackPressed();
                }
            });
        }

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(mFragmentActivity)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();

        return mRootView;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = mRootView.findViewById(R.id.article_title);
        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        TextView bodyView = mRootView.findViewById(R.id.article_body);

        if (mCursor != null) {
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));

            }
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)
                    .replaceAll("(\r\n\r\n)", "<br><br>").replaceAll("(\r\n)", " ")));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {

                                new Palette.Builder(bitmap).generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(@NonNull Palette palette) {
                                        if (getActivity() != null) {
                                            int newMetaBarColor = palette.getDarkMutedColor(getResources()
                                                    .getColor(R.color.colorPrimaryDark));
                                            int newFabButtonColor = palette.getLightVibrantColor(getResources()
                                                    .getColor(R.color.colorPrimary));
                                            int newBackgroundColor = palette.getLightMutedColor(getResources()
                                                    .getColor(R.color.colorPrimaryLight));


                                            mRootView.findViewById(R.id.article_fragment_container)
                                                    .setBackgroundColor(newBackgroundColor);
                                            mRootView.findViewById(R.id.share_fab)
                                                    .setBackgroundTintList(ColorStateList.valueOf(newFabButtonColor));
                                            mRootView.findViewById(R.id.meta_bar)
                                                    .setBackgroundColor(newMetaBarColor);
                                        }
                                    }
                                });
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
        } else {
            mRootView.setVisibility(View.INVISIBLE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
