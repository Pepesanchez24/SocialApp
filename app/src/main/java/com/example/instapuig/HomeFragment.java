package com.example.instapuig;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class HomeFragment extends Fragment {
    NavController navController;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;
    Client client;
    Account account;
    String userId;
    PostsAdapter adapter;
    AppViewModel appViewModel;


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle
            savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        NavigationView navigationView =
                view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);
        client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    return;
                }
                mainHandler.post(() ->
                {
                    assert result != null;
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
        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(v -> navController.navigate(R.id.fragment_new_post));
        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);

        adapter = new PostsAdapter();
        postsRecyclerView.setAdapter(adapter);
        appViewModel = new
                ViewModelProvider(requireActivity()).get(AppViewModel.class);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    static class PostViewHolder extends RecyclerView.ViewHolder{
        ImageView authorPhotoImageView, likeImageView, mediaImageView, deleteImageView;
        TextView authorTextView, contentTextView, numLikesTextView, hashtagsTextView;
        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            deleteImageView = itemView.findViewById(R.id.deleteImageView);
            hashtagsTextView = itemView.findViewById(R.id.hashtagsTextView);
        }
    }

    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
        DocumentList<Map<String, Object>> lista = null;

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_viewholder_post, parent, false));
        }

        @SuppressLint("UseRequireInsteadOfGet")
        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Map<String, Object> post = lista.getDocuments().get(position).getData();

            // Autor
            String author = post.get("author") != null ? Objects.requireNonNull(post.get("author")).toString() : "Autor desconocido";
            holder.authorTextView.setText(author);

            // Contenido
            String content = post.get("content") != null ? Objects.requireNonNull(post.get("content")).toString() : "";
            holder.contentTextView.setText(content);

            // Hashtags
            List<String> hashtags = new ArrayList<>();
            if (post.get("hashtags") instanceof List) {
                //noinspection unchecked
                hashtags = (List<String>) post.get("hashtags");
            }
            StringBuilder hashtagsText = new StringBuilder();
            assert hashtags != null;
            for (String hashtag : hashtags) {
                hashtagsText.append("#").append(hashtag).append(" ");
            }
            holder.hashtagsTextView.setText(hashtagsText.toString());
            holder.hashtagsTextView.setTextColor(Color.parseColor("#CBA0FF")); // Lila

            // Foto del autor
            if (post.get("authorPhotoUrl") == null) {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            } else {
                Glide.with(Objects.requireNonNull(getContext()))
                        .load(Objects.requireNonNull(post.get("authorPhotoUrl")).toString())
                        .circleCrop()
                        .into(holder.authorPhotoImageView);
            }

            // Media
            if (post.get("mediaUrl") != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(Objects.requireNonNull(post.get("mediaType")).toString())) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(Objects.requireNonNull(post.get("mediaUrl")).toString()).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            // Likes
            List<String> likes = new ArrayList<>();
            if (post.get("likes") instanceof List) {
                //noinspection unchecked
                likes = (List<String>) post.get("likes");
            }

            assert likes != null;
            boolean userLiked = likes.contains(userId);
            holder.likeImageView.setImageResource(userLiked ? R.drawable.like_on : R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(likes.size()));

            List<String> finalLikes = new ArrayList<>(likes); // para el uso en lambda
            holder.likeImageView.setOnClickListener(v -> {
                Databases databases = new Databases(client);
                String postId = Objects.requireNonNull(post.get("$id")).toString();

                if (finalLikes.contains(userId)) {
                    finalLikes.remove(userId);
                } else {
                    finalLikes.add(userId); // Dar like
                }

                Map<String, Object> data = new HashMap<>();
                data.put("likes", finalLikes);

                try {
                    databases.updateDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            postId,
                            data,
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    Snackbar.make(requireView(), "Error al actualizar likes: " + error.getMessage(), Snackbar.LENGTH_LONG).show();
                                    return;
                                }

                                new Handler(Looper.getMainLooper()).post(() -> {
                                    post.put("likes", finalLikes);
                                    notifyItemChanged(holder.getAdapterPosition());
                                });
                            })
                    );
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }
            });

            // Eliminar (solo si es el autor)
            String authorId = post.get("authorId") != null ? Objects.requireNonNull(post.get("authorId")).toString() : null;
            if (userId.equals(authorId)) {
                holder.deleteImageView.setVisibility(View.VISIBLE);
            } else {
                holder.deleteImageView.setVisibility(View.GONE);
            }
            holder.deleteImageView.setOnClickListener(view -> {
                String postId = Objects.requireNonNull(post.get("$id")).toString();
                eliminarPost(postId, authorId);
            });
        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        @SuppressLint("NotifyDataSetChanged")
        void establecerLista(DocumentList<Map<String, Object>> lista) {
            this.lista = lista;
            notifyDataSetChanged();
        }

        void eliminarPost(String postId, String authorId) {
            if (!userId.equals(authorId)) {
                Snackbar.make(requireView(), "No tienes permisos para eliminar este post", Snackbar.LENGTH_LONG).show();
                return;
            }

            Databases databases = new Databases(client);
            Handler mainHandler = new Handler(Looper.getMainLooper());

            databases.deleteDocument(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                    postId,
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al eliminar el post: " + error.getMessage(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        mainHandler.post(HomeFragment.this::obtenerPosts);
                    })
            );
        }
    }

    void obtenerPosts()
    {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID), // collectionId
                    new ArrayList<>(), // queries (optional)
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts: "
                                    + error, Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        assert result != null;
                        System.out.println(result);
                        mainHandler.post(() -> adapter.establecerLista(result));
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

}