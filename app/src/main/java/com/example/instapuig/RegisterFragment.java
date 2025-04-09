package com.example.instapuig;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

import io.appwrite.Client;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.services.Account;
import io.appwrite.coroutines.CoroutineCallback;

public class RegisterFragment extends Fragment {
    NavController navController;
    private Button registerButton;
    private EditText usernameEditText, emailEditText, passwordEditText;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        registerButton = view.findViewById(R.id.registerButton);

        registerButton.setOnClickListener(view1 -> crearCuenta());
    }

    private void crearCuenta() {
        if (!validarFormulario()) {
            return;
        }

        registerButton.setEnabled(false);

        Client client = new Client(requireActivity().getApplicationContext());
        client.setProject(getString(R.string.APPWRITE_PROJECT_ID));
        Account account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            account.create(
                    "unique()",
                    emailEditText.getText().toString(), // email
                    passwordEditText.getText().toString(), // password
                    usernameEditText.getText().toString(),
                    new CoroutineCallback<>((result, error) -> {
                        mainHandler.post(() -> registerButton.setEnabled(true));

                        if (error != null) {
                            Snackbar.make(requireView(), "Error: " + error, Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        // Crear sesión para el usuario
                        account.createEmailPasswordSession(
                                emailEditText.getText().toString(),
                                passwordEditText.getText().toString(),
                                new CoroutineCallback<>((sessionResult, sessionError) -> {
                                    if (sessionError != null) {
                                        Snackbar.make(requireView(), "Error: " + sessionError, Snackbar.LENGTH_LONG).show();
                                    } else {
                                        mainHandler.post(this::actualizarUI);
                                    }
                                })
                        );
                    })
            );
        } catch (AppwriteException e) {
            registerButton.setEnabled(true);
            Snackbar.make(requireView(), "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void actualizarUI() {
        navController.navigate(R.id.homeFragment);
    }

    private boolean validarFormulario() {
        boolean valid = true;

        if (TextUtils.isEmpty(emailEditText.getText().toString())) {
            emailEditText.setError("Required.");
            valid = false;
        } else {
            emailEditText.setError(null);
        }

        if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
            passwordEditText.setError("Required.");
            valid = false;
        } else {
            passwordEditText.setError(null);
        }

        if (TextUtils.isEmpty(usernameEditText.getText().toString())) {
            usernameEditText.setError("Required.");
            valid = false;
        } else {
            usernameEditText.setError(null);
        }

        return valid;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }
}