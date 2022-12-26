package com.example.HtmlEditor;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
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
    private Uri uri = null;

    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        mEditText = findViewById(R.id.textViewCode);
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
                alterDocument(uri, content);
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
            uri = null;
            return true;
        } else if (item.getItemId() == R.id.action_template) {
            openTemplate();
            uri = null;
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            // TODO
            return true;
        } else {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            return super.onOptionsItemSelected(item);
        }
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
            uri = null;
            if (data != null) {
                uri = data.getData();
                String fileContent = null;
                try {
                    fileContent = readTextFromUri(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mEditText.setText(fileContent);
                Toast.makeText(this, "File opened successfully", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CREATE_FILE && resultCode == RESULT_OK) {
            uri = null;
            if (data != null) {
                uri = data.getData();
                String content = mEditText.getText().toString();
                alterDocument(uri, content);
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
}