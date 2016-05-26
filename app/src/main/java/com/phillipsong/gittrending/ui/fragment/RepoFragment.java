/*
 * Copyright (c) 2016 Phillip Song (http://github.com/awind).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.phillipsong.gittrending.ui.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.jakewharton.rxbinding.support.v4.widget.RxSwipeRefreshLayout;
import com.phillipsong.gittrending.R;
import com.phillipsong.gittrending.TrendingApplication;
import com.phillipsong.gittrending.data.api.TrendingService;
import com.phillipsong.gittrending.data.models.Repo;
import com.phillipsong.gittrending.inject.components.AppComponent;
import com.phillipsong.gittrending.inject.components.DaggerRepoFragmentComponent;
import com.phillipsong.gittrending.inject.modules.RepoFragmentModule;
import com.phillipsong.gittrending.ui.adapter.RepoAdapter;
import com.phillipsong.gittrending.ui.misc.OnItemClickListener;
import com.phillipsong.gittrending.ui.widget.PSwipeRefreshLayout;
import com.phillipsong.gittrending.ui.widget.StringPickerDialog;
import com.phillipsong.gittrending.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class RepoFragment extends BaseFragment implements OnItemClickListener, View.OnClickListener {

    private static final String TAG = "RepoFragment";

    private Toolbar mToolbar;
    private TextView mTitleTv;
    private ImageButton mLangBtn;
    private ImageButton mSinceBtn;
    private PSwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private RepoAdapter mRepoAdapter;
    private List<Repo> mRepoList;

    @Inject
    TrendingApplication mContext;
    @Inject
    TrendingService mTrendingApi;
    @Inject
    SharedPreferences mSharedPreferences;

    private String mLanguage;
    private String mSince;


    private Action1<List<Repo>> mUpdateAction = items -> {
        mRepoList.clear();
        mRepoList.addAll(items);
        mRepoAdapter.notifyDataSetChanged();
    };

    private Action1<Throwable> mThrowableAction = throwable -> {
            Log.d(TAG, throwable.getLocalizedMessage());
            Answers.getInstance().logCustom(new CustomEvent("UpdateException")
            .putCustomAttribute("location", TAG)
            .putCustomAttribute("message", throwable.getMessage()));
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLanguage = mSharedPreferences.getString(Constants.REPO_LANG_KEY, "All");
        mSince = "Daily";
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repo, container, false);
        initViews(view);
        return view;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {
        DaggerRepoFragmentComponent.builder()
                .appComponent(appComponent)
                .repoFragmentModule(new RepoFragmentModule(this))
                .build()
                .inject(this);
    }

    private void initViews(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mTitleTv = (TextView) view.findViewById(R.id.title);
        mTitleTv.setText(mLanguage);

        mLangBtn = (ImageButton) view.findViewById(R.id.language_btn);
        mSinceBtn = (ImageButton) view.findViewById(R.id.since_btn);
        mLangBtn.setOnClickListener(this);
        mSinceBtn.setOnClickListener(this);

        mSwipeRefreshLayout = (PSwipeRefreshLayout) view.findViewById(R.id.refresher);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mRecyclerView.setNestedScrollingEnabled(true);
        }
        mRepoList = new ArrayList<>();
        mRepoAdapter = new RepoAdapter(mContext, mRepoList, this);
        mRecyclerView.setAdapter(mRepoAdapter);

        updateData(mLanguage, mSince);

        RxSwipeRefreshLayout.refreshes(mSwipeRefreshLayout)
                .compose(bindToLifecycle())
                .observeOn(Schedulers.io())
                .flatMap(aVoid -> mTrendingApi.getTrending(mLanguage.toLowerCase(), mSince.toLowerCase()))
                .retry(2)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> mSwipeRefreshLayout.setRefreshing(false))
                .doOnNext(aVoid -> mSwipeRefreshLayout.setRefreshing(false))
                .doOnCompleted(() -> mSwipeRefreshLayout.setRefreshing(false))
                .retry()
                .flatMap(response -> Observable.just(response.getItems()))
                .subscribe(mUpdateAction, mThrowableAction);
    }

    public void updateData(String language, String since) {
        mLanguage = language;
        mSince = since;
        mTrendingApi.getTrending(mLanguage.toLowerCase(), mSince.toLowerCase())
                .compose(bindToLifecycle())
                .retry(2)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(() -> mSwipeRefreshLayout.setRefreshing(true))
                .doOnCompleted(() -> mSwipeRefreshLayout.setRefreshing(false))
                .doOnError(error -> mSwipeRefreshLayout.setRefreshing(false))
                .flatMap(response -> Observable.just(response.getItems()))
                .subscribe(mUpdateAction, mThrowableAction);
    }


    @Override
    public void onItemClick(int position) {
        if (mRepoList == null || mRepoList.size() == 0) {
            return;
        }
        Repo repo = mRepoList.get(position);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(Constants.GITHUB_BASE_URL + repo.getUrl()));
        startActivity(i);

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName(repo.getUrl())
                .putContentType(TAG));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int id = item.getItemId();
        switch (id) {
            case R.id.action_daily:
                Log.d(TAG, "onOptionsItemSelected: 1");
                break;
            case R.id.action_weekly:
                Log.d(TAG, "onOptionsItemSelected: 2");
                break;
            case R.id.action_monthly:
                Log.d(TAG, "onOptionsItemSelected: 3");
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.language_btn:
                chooseLanguage();
                break;
            case R.id.since_btn:
                showSinceDialog();
                break;
        }
    }

    private void chooseLanguage() {
        StringPickerDialog dialog = new StringPickerDialog();
        dialog.setListener(value -> {
            if (!value.equals(mLanguage)) {
                mLanguage = value;
                mTitleTv.setText(mLanguage);
                mSharedPreferences.edit().putString(Constants.REPO_LANG_KEY, mLanguage).apply();
                updateData(mLanguage, mSince);
            }
        });
        Bundle bundle = new Bundle();
        String[] languages = getResources().getStringArray(R.array.support_languages);
        bundle.putStringArray(getString(R.string.string_picker_dialog_values), languages);
        int index = Arrays.asList(languages).indexOf(mLanguage);
        bundle.putInt(getString(R.string.string_picker_dialog_current_index), index);
        dialog.setArguments(bundle);
        dialog.show(getChildFragmentManager(), TAG);
    }

    private void showSinceDialog() {
        final String[] sinceArray = getResources().getStringArray(R.array.lang_since_array);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(),
                R.style.AppCompatAlertDialogStyle);
        dialogBuilder.setTitle(mContext.getString(R.string.fragment_repo_choose_since));
        dialogBuilder.setItems(sinceArray, ((dialog, which) -> {
            String since = sinceArray[which];
            if (!since.equals(mSince)) {
                mSince = since;
                updateData(mLanguage, mSince);
            }
        }));
        AlertDialog alertDialogObject = dialogBuilder.create();
        alertDialogObject.show();
    }
}