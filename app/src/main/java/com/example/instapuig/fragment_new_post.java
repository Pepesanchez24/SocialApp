package com.example.instapuig;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.InputFile;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Storage;

public class fragment_new_post extends Fragment {

    Button publishButton;
    EditText postContentEditText;
    NavController navController;
    Client client;
    Account account;

    AppViewModel appViewModel;
    Uri mediaUri;
    String mediaTipo;

    private final ActivityResultLauncher<String> galeria = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> appViewModel.setMediaSeleccionado(uri, mediaTipo));
    private final ActivityResultLauncher<Uri> camaraFotos = registerForActivityResult(new ActivityResultContracts.TakePicture(),
            isSuccess -> appViewModel.setMediaSeleccionado(mediaUri, "image"));
    private final ActivityResultLauncher<Uri> camaraVideos = registerForActivityResult(new ActivityResultContracts.TakeVideo(),
            isSuccess -> appViewModel.setMediaSeleccionado(mediaUri, "video"));
    private final ActivityResultLauncher<Intent> grabadoraAudio = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            assert result.getData() != null;
                            appViewModel.setMediaSeleccionado(result.getData().getData(), "audio");
                        }
                    });

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

        publishButton.setOnClickListener(v -> publicar());

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        view.findViewById(R.id.camara_fotos).setOnClickListener(v -> tomarFoto());
        view.findViewById(R.id.camara_video).setOnClickListener(v -> tomarVideo());
        view.findViewById(R.id.grabar_audio).setOnClickListener(v -> grabarAudio());
        view.findViewById(R.id.imagen_galeria).setOnClickListener(v -> seleccionarImagen());
        view.findViewById(R.id.video_galeria).setOnClickListener(v -> seleccionarVideo());
        view.findViewById(R.id.audio_galeria).setOnClickListener(v -> seleccionarAudio());

        appViewModel.mediaSeleccionado.observe(getViewLifecycleOwner(), media -> {
            this.mediaUri = media.uri;
            this.mediaTipo = media.tipo;
            Glide.with(this).load(media.uri).into((ImageView) view.findViewById(R.id.previsualizacion));
        });
    }

    private void seleccionarImagen() {
        mediaTipo = "image";
        galeria.launch("image/*");
    }

    private void seleccionarVideo() {
        mediaTipo = "video";
        galeria.launch("video/*");
    }

    private void seleccionarAudio() {
        mediaTipo = "audio";
        galeria.launch("audio/*");
    }

    private void tomarFoto() {
        try {
            mediaUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.socialappwrite.fileprovider",
                    File.createTempFile("img", ".jpg", requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)));
            camaraFotos.launch(mediaUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void tomarVideo() {
        try {
            mediaUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.socialappwrite.fileprovider",
                    File.createTempFile("vid", ".mp4", requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)));
            camaraVideos.launch(mediaUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void grabarAudio() {
        grabadoraAudio.launch(new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION));
    }

    private void publicar() {
        String postContent = postContentEditText.getText().toString();
        if (TextUtils.isEmpty(postContent)) {
            postContentEditText.setError("Required");
            return;
        }

        publishButton.setEnabled(false);

        EditText hashtagsEditText = requireView().findViewById(R.id.hashtagsEditText);
        String hashtagsInput = hashtagsEditText.getText().toString();
        List<String> hashtags = new ArrayList<>();
        if (!hashtagsInput.isEmpty()) {
            for (String hashtag : hashtagsInput.split(" ")) {
                if (!hashtag.isEmpty()) hashtags.add(hashtag);
            }
        }

        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) return;
                if (mediaTipo == null) {
                    assert result != null;
                    guardarEnAppWrite(result, postContent, null, hashtags);
                } else {
                    pujaIguardarEnAppWrite(result, postContent, hashtags);
                }
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    private void pujaIguardarEnAppWrite(User<Map<String, Object>> user, String postText, List<String> hashtags) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Storage storage = new Storage(client);
        File tempFile;
        try {
            tempFile = getFileFromUri(requireContext(), mediaUri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        storage.createFile(
                getString(R.string.APPWRITE_STORAGE_BUCKET_ID),
                "unique()",
                InputFile.Companion.fromFile(tempFile),
                new ArrayList<>(),
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        System.err.println("Error subiendo el archivo: " + error.getMessage());
                        return;
                    }
                    assert result != null;
                    String downloadUrl = "https://cloud.appwrite.io/v1/storage/buckets/" +
                            getString(R.string.APPWRITE_STORAGE_BUCKET_ID) + "/files/" + result.getId() +
                            "/view?project=" + getString(R.string.APPWRITE_PROJECT_ID) + "&mode=admin";
                    mainHandler.post(() -> guardarEnAppWrite(user, postText, downloadUrl, hashtags));
                }));
    }

    private void guardarEnAppWrite(User<Map<String, Object>> user, String content, String mediaUrl, List<String> hashtags) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Databases databases = new Databases(client);
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getId());
        data.put("author", user.getName());
        data.put("authorPhotoUrl", null);
        data.put("content", content);
        data.put("mediaType", mediaTipo);
        data.put("mediaUrl", mediaUrl);
        data.put("authorId", user.getId());
        data.put("hashtags", hashtags);

        try {
            databases.createDocument(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                    "unique()",
                    data,
                    new ArrayList<>(),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error: " + error, Snackbar.LENGTH_LONG).show();
                        } else {
                            mainHandler.post(() -> navController.popBackStack());
                        }
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

    public File getFileFromUri(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new FileNotFoundException("No se pudo abrir el URI: " + uri);
        String fileName = getFileName(context, uri);
        File tempFile = new File(context.getCacheDir(), fileName);
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    private String getFileName(Context context, Uri uri) {
        String fileName = "temp_file";
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        return fileName;
    }
}