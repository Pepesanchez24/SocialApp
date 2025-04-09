package com.example.instapuig;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.services.Account;

public class SignInFragment extends Fragment {
    NavController navController;
    Client client;
    Account account;
    private EditText emailEditText, passwordEditText;
    private LinearLayout signInForm;
    private ProgressBar signInProgressBar;

    public SignInFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        Button emailSignInButton = view.findViewById(R.id.emailSignInButton);
        TextView gotoCreateAccountTextView = view.findViewById(R.id.gotoCreateAccountTextView);
        signInForm = view.findViewById(R.id.signInForm);
        signInProgressBar = view.findViewById(R.id.signInProgressBar);

        gotoCreateAccountTextView.setOnClickListener(v -> navController.navigate(R.id.registerFragment));

        Handler mainHandler = new Handler(Looper.getMainLooper());
        client = new Client(requireActivity().getApplicationContext());
        client.setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        account.getSession(
                "current", // sessionId
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        return;
                    }
                    if(result != null)
                        mainHandler.post(this::actualizarUI);
                })
        );

        emailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accederConEmail();
            }

            private void accederConEmail() {
                signInForm.setVisibility(View.GONE);
                signInProgressBar.setVisibility(View.VISIBLE);
                Account account = new Account(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                account.createEmailPasswordSession(
                        emailEditText.getText().toString(), // email
                        passwordEditText.getText().toString(), // password
                        new CoroutineCallback<>((result, error) -> {
                            if (error != null) {
                                Snackbar.make(requireView(), "Error: " +
                                        error, Snackbar.LENGTH_LONG).show();
                            } else {
                                assert result != null;
                                System.out.println("Sesión creada para el usuario:" + result);
                                mainHandler.post(() -> actualizarUI());
                            }
                            mainHandler.post(() -> {
                                signInForm.setVisibility(View.VISIBLE);
                                signInProgressBar.setVisibility(View.GONE);
                            });
                        })
                );
            }
        });
    }

    private void actualizarUI() {
        navController.navigate(R.id.homeFragment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
    }
}
