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
package com.phillipsong.gittrending.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.phillipsong.gittrending.R;
import com.phillipsong.gittrending.data.models.Repo;
import com.phillipsong.gittrending.ui.misc.OnRepoItemClickListener;
import com.phillipsong.gittrending.ui.widget.AvatarContainer;

import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.RepoViewHolder> {

    public static class RepoViewHolder extends RecyclerView.ViewHolder {
        private TextView mOwner;
        private TextView mTitle;
        private ImageView mLanguage;
        private TextView mDescription;
        private AvatarContainer mAvatar;
        private TextView mStar;
        private TextView mShare;
        private TextView mFavorite;

        public RepoViewHolder(View view) {
            super(view);
            mTitle = (TextView) view.findViewById(R.id.title);
            mLanguage = (ImageView) view.findViewById(R.id.language);
            mDescription = (TextView) view.findViewById(R.id.description);
            mAvatar = (AvatarContainer) view.findViewById(R.id.avatar);
            mStar = (TextView) view.findViewById(R.id.star);
            mShare = (TextView) view.findViewById(R.id.share);
            mFavorite = (TextView) view.findViewById(R.id.favorite);
        }

        public void bind(Context context, Repo repo, int position, OnRepoItemClickListener listener) {
            SpannableString name = new SpannableString(repo.getName());
            name.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, name.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTitle.setText(String.format(context.getString(R.string.repo_title),
                    repo.getOwner()));
            mTitle.append(name);
            String languagePic = String.format(context.getString(R.string.image_base_url),
                    repo.getLanguage().toLowerCase());
            Glide.with(context)
                    .load(languagePic)
                    .error(R.mipmap.ic_lang)
                    .fitCenter()
                    .into(mLanguage);
            mDescription.setText(repo.getDescription());
            mAvatar.setImageUrls(repo.getContributors());
            mStar.setText(repo.getStar());

            if (repo.isFavorited()) {
                mFavorite.setText(R.string.repo_item_liked);
            } else {
                mFavorite.setText(R.string.repo_item_like);
            }

            mShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(position);
                }
            });
            mFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(position);
                }
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(position);
                }
            });
        }
    }

    private Context mContext;
    private List<Repo> mRepos;
    private OnRepoItemClickListener mListener;

    public RepoAdapter(Context context, List<Repo> list, OnRepoItemClickListener listener) {
        this.mContext = context;
        this.mRepos = list;
        this.mListener = listener;
    }

    @Override
    public RepoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repo,
                parent, false);
        RepoViewHolder viewHolder = new RepoViewHolder(view);
        view.setTag(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RepoViewHolder holder, int position) {
        Repo repo = mRepos.get(position);
        holder.bind(mContext, repo, position, mListener);
    }

    @Override
    public int getItemCount() {
        return mRepos == null ? 0 : mRepos.size();
    }

    public void setItemClickListener(OnRepoItemClickListener listener) {
        this.mListener = listener;
    }
}