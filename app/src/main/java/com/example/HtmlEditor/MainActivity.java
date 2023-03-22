package com.example.HtmlEditor;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TEMPLATE = "template.html";
    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 42;
    private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 1000;
    private static final int PICK_FILE = 2;
    private static final int CREATE_FILE = 1;
    private Uri mUri = null;
    private String mUriString = "";
    private EditText mEditText;
    private TextView mEditTextLineCount;
    // Shared Preferences
    private SharedPreferences mSharedPreferences;
    private final String mSharedPreferencesFile = "com.thpir.myenergydesk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        // Initialize the shared preferences
        mSharedPreferences = getSharedPreferences(mSharedPreferencesFile, MODE_PRIVATE);
        mUriString = mSharedPreferences.getString("URI", "");

        // Initialize remaining widgets
        mEditTextLineCount = findViewById(R.id.textViewLineNumbers);
        mEditText = findViewById(R.id.textViewCode);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Nothing to do
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Nothing to do
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String lineCount = "";
                String text = mEditText.getText().toString();
                String[] lines = text.split("\n");
                for (int j = 1; j <= lines.length; j++) {
                    lineCount = lineCount + j + "\n";
                }
                mEditTextLineCount.setText(lineCount);
            }
        });

        // reset the URI so we start the application with an empty screen
        mUri = null;

        // FAB setup
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            if (mUri != null) {
                Intent intent = new Intent(MainActivity.this, HtmlViewerActivity.class);
                intent.putExtra("uri", mUri.toString());
                startActivity(intent);
            } else {
                Snackbar.make(view, "First open or save a document", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUriString = "";
        if (mUri!= null) mUriString = mUri.toString();
        savedSharedPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_open) {
            readStoragePermission();
            openFile();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            String content = mEditText.getText().toString();
            try {
                alterDocument(mUri, content);
            } catch (Exception e) {
                e.printStackTrace();
                writeStoragePermission();
                createFile();
            }
            return true;
        } else if (item.getItemId() == R.id.action_save_as) {
            writeStoragePermission();
            createFile();
            return true;
        } else if (item.getItemId() == R.id.action_new) {
            mEditText.setText("");
            mUri = null;
            return true;
        } else if (item.getItemId() == R.id.action_template) {
            openTemplate();
            mUri = null;
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            inflatePopUp();
            return true;
        } else {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            return super.onOptionsItemSelected(item);
        }
    }

    // Alert dialog with app info
    private void inflatePopUp() {
        new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("About")
                .setView(R.layout.popup_about)
                .setNegativeButton("OK", null)
                .show();
    }

    private void readStoragePermission() {
        if(!checkPermissionForReadExternalStorage()) {
            try {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_STORAGE_PERMISSION_REQUEST_CODE);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    private void writeStoragePermission() {
        if(!checkPermissionForWriteExternalStorage()) {
            try {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_STORAGE_PERMISSION_REQUEST_CODE);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    private boolean checkPermissionForReadExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    private boolean checkPermissionForWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    // Open a html boilerplate from the assets folder
    private void openTemplate() {
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(getAssets().open(TEMPLATE)));
            StringBuilder stringBuilder = new StringBuilder();
            String text;

            while ((text = bufferedReader.readLine()) != null) {
                stringBuilder.append(text).append("\n");
            }

            mEditText.setText(stringBuilder.toString());
            mUri = null;
            mUriString = "";
            savedSharedPreferences();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Select a file you want to open
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, PICK_FILE);
    }

    // Create a file in a specific location
    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, "test.html");
        startActivityForResult(intent, CREATE_FILE);
    }

    // Perform action on the chosen location when firing the create or open document intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE && resultCode == RESULT_OK) {
            mUri = null;
            if (data != null) {
                mUri = data.getData();
                mUriString = mUri.toString();
                savedSharedPreferences();
                String fileContent = null;
                try {
                    fileContent = readTextFromUri(mUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mEditText.setText(fileContent);
                Toast.makeText(this, "File opened successfully", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CREATE_FILE && resultCode == RESULT_OK) {
            mUri = null;
            if (data != null) {
                mUri = data.getData();
                mUriString = mUri.toString();
                savedSharedPreferences();
                String content = mEditText.getText().toString();
                alterDocument(mUri, content);
            }
        }
    }

    // Change the file content of the selected Uri
    private void alterDocument(Uri uri, String content) {
        try {
            ParcelFileDescriptor pfd = this.getContentResolver().
                    openFileDescriptor(uri, "rwt");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());
            fileOutputStream.write(content.getBytes());
            // Let the document provider know you're done by closing the stream.
            fileOutputStream.close();
            pfd.close();
            Toast.makeText(this, "File saved!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file", Toast.LENGTH_LONG).show();
        }
    }

    // Read the file and display content
    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream =
                     getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private void savedSharedPreferences() {
        // get and editor for the SharePreferences object
        mSharedPreferences = getSharedPreferences(mSharedPreferencesFile, MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putString("URI", mUriString);
        sharedPreferencesEditor.apply();
    }
}