package com.example.socialapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import java.util.HashMap;
import java.util.Map;
import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class fragment_new_post extends Fragment {
    private Button publishButton;
    private EditText postContentEditText;
    private NavController navController;
    private Client client;
    private Account account;
    private AppViewModel appViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        publishButton = view.findViewById(R.id.publishButton);
        postContentEditText = view.findViewById(R.id.postContentEditText);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        publishButton.setOnClickListener(v -> publicar());
    }

    private void publicar() {
        String postContent = postContentEditText.getText().toString();
        if (TextUtils.isEmpty(postContent)) {
            postContentEditText.setError("Requerido");
            return;
        }
        publishButton.setEnabled(false);

        try {
            account.get(new CoroutineCallback<User<Map<String, Object>>>((result, error) -> {
                if (error != null) {
                    Snackbar.make(requireView(), "Error: " + error.toString(), Snackbar.LENGTH_LONG).show();
                    return;
                }
                guardarEnAppWrite(result, postContent);
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void guardarEnAppWrite(User<Map<String, Object>> user, String content) {
        Databases databases = new Databases(client);
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getId());
        data.put("author", user.getName());
        data.put("content", content);

        try {
            databases.createDocument(getString(R.string.APPWRITE_DATABASE_ID), getString(R.string.APPWRITE_POSTS_COLLECTION_ID), "unique()", data);
            navController.popBackStack();
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }
}
