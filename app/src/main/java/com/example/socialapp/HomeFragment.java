package com.example.socialapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class HomeFragment extends Fragment {

    AppViewModel appViewModel;
    NavController navController;
    PostsAdapter adapter;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;
    Client client;
    Account account;
    Databases databases;
    String userId;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);

        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        databases = new Databases(client);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    userId = result.getId();
                    displayNameTextView.setText(result.getName());
                    emailTextView.setText(result.getEmail());
                    Glide.with(requireView()).load(R.drawable.user).into(photoImageView);
                    obtenerPosts();
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }

        navController = Navigation.findNavController(view);
        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        adapter = new PostsAdapter(this);
        postsRecyclerView.setAdapter(adapter);

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(v ->
                navController.navigate(R.id.fragment_new_post)
        );
    }

    public HomeFragment() {}

    class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView authorPhotoImageView, likeImageView, mediaImageView, deleteImageView;
        TextView authorTextView, contentTextView, numLikesTextView, tagsTextView;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            deleteImageView = itemView.findViewById(R.id.deleteImageView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            tagsTextView = itemView.findViewById(R.id.tagsTextView);
        }
    }

    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
        DocumentList<Map<String, Object>> lista = null;
        private final HomeFragment homeFragment;

        public PostsAdapter(HomeFragment homeFragment) {
            this.homeFragment = homeFragment;
        }

        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_viewholder_post, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Map<String, Object> post = lista.getDocuments().get(position).getData();

            holder.authorTextView.setText(post.get("author").toString());
            holder.contentTextView.setText(post.get("content").toString());

            List<String> likes = (List<String>) post.get("likes");
            holder.likeImageView.setImageResource(likes.contains(userId) ? R.drawable.like_on : R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(likes.size()));

            holder.likeImageView.setOnClickListener(view -> homeFragment.actualizarLikes(post, likes));

            List<String> tags = (List<String>) post.get("tags");
            if (tags != null && !tags.isEmpty()) {
                holder.tagsTextView.setText("#" + String.join(" #", tags));
                holder.tagsTextView.setVisibility(View.VISIBLE);
            } else {
                holder.tagsTextView.setVisibility(View.GONE);
            }

            if (post.get("authorId").toString().equals(userId)) {
                holder.deleteImageView.setVisibility(View.VISIBLE);
                holder.deleteImageView.setOnClickListener(view -> homeFragment.eliminarPost(post.get("$id").toString()));
            } else {
                holder.deleteImageView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        public void establecerLista(DocumentList<Map<String, Object>> lista) {
            this.lista = lista;
            notifyDataSetChanged();
        }
    }

    void obtenerPosts() {
        try {
            databases.listDocuments("database_id", "collection_id", new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                requireActivity().runOnUiThread(() -> adapter.establecerLista(result));
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    void actualizarLikes(Map<String, Object> post, List<String> likes) {
        String postId = post.get("$id").toString();
        if (likes.contains(userId)) {
            likes.remove(userId);
        } else {
            likes.add(userId);
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("likes", likes);

        try {
            databases.updateDocument("database_id", "collection_id", postId, updates, new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                requireActivity().runOnUiThread(() -> obtenerPosts());
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    void eliminarPost(String postId) {
        databases.deleteDocument("database_id", "collection_id", postId, new CoroutineCallback<>((result, error) -> {
            if (error != null) {
                error.printStackTrace();
                return;
            }
            requireActivity().runOnUiThread(() -> {
                obtenerPosts();
                Snackbar.make(requireView(), "Post eliminado", Snackbar.LENGTH_SHORT).show();
            });
        }));
    }
}
