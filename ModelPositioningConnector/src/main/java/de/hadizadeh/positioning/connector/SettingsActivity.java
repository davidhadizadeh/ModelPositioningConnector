package de.hadizadeh.positioning.connector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import de.hadizadeh.positioning.roommodel.FileManager;
import de.hadizadeh.positioning.roommodel.android.Settings;

import java.util.List;

/**
 * Manages the current project settings of the application
 */
public class SettingsActivity extends Activity {
    /**
     * Request code for starting this activity for result
     */
    public static final int ACTIVITY_REQUEST_CODE = 1;

    protected EditText webserviceEt;
    protected EditText tokenEt;
    protected Button discardBtn;
    protected Button saveBtn;

    protected Spinner projectSp;
    protected ArrayAdapter<String> projectSpAdapter;
    protected SharedPreferences sharedPref;

    /**
     * Creates the view and loads the current settings
     *
     * @param savedInstanceState saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        webserviceEt = (EditText) findViewById(R.id.activity_settings_webservice_et);
        tokenEt = (EditText) findViewById(R.id.activity_settings_token_et);
        projectSp = (Spinner) findViewById(R.id.activity_settings_project_sp);
        discardBtn = (Button) findViewById(R.id.activity_settings_discard_btn);
        saveBtn = (Button) findViewById(R.id.activity_settings_save_btn);

        discardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        sharedPref = getSharedPreferences(Settings.PREFERENCE_FILE, Context.MODE_PRIVATE);
        webserviceEt.setText(sharedPref.getString(Settings.WEBSERVICE, getString(R.string.settings_default_webservice)));
        tokenEt.setText(sharedPref.getString(Settings.TOKEN, getString(R.string.settings_default_token)));
        loadProjects();
    }

    /**
     * Loads all available project names
     */
    protected void loadProjects() {
        if(saveBtn != null) {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
        }
        new DownloadProjectsTask(this, sharedPref.getString(Settings.PROJECT, getString(R.string.settings_default_project))).execute(webserviceEt.getText().toString());
    }

    /**
     * Called, when the project list has been loaded
     *
     * @param projects        available projects
     * @param selectedProject current selected project
     */
    protected void projectsLoaded(List<String> projects, String selectedProject) {
        projectSpAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
        projectSpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        projectSp.setAdapter(projectSpAdapter);
        projectSpAdapter.clear();
        int selectedIndex = 0;
        for (int i = 0; i < projects.size(); i++) {
            projectSpAdapter.add(projects.get(i));
            if (projects.get(i).equals(selectedProject)) {
                selectedIndex = i;
            }
        }
        projectSpAdapter.notifyDataSetChanged();
        projectSp.setSelection(selectedIndex);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(Settings.WEBSERVICE, webserviceEt.getText().toString());
                editor.putString(Settings.TOKEN, tokenEt.getText().toString());
                editor.putString(Settings.PROJECT, (String) projectSp.getSelectedItem());
                editor.commit();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /**
     * Downloads the project information
     */
    protected class DownloadProjectsTask extends AsyncTask<String, List<String>, List<String>> {
        protected SettingsActivity settingsActivity;
        protected String selectedProject;

        /**
         * Initializes the callback reference and the selected project
         *
         * @param settingsActivity settings activity reference
         * @param selectedProject selected project
         */
        public DownloadProjectsTask(SettingsActivity settingsActivity, String selectedProject) {
            this.settingsActivity = settingsActivity;
            this.selectedProject = selectedProject;
        }

        /**
         * Downloads the project list
         *
         * @param data data with field 0: url to the server and project subresource
         * @return list of projects
         */
        @Override
        protected List<String> doInBackground(String... data) {
            List<String> projects = FileManager.getAllProjects(data[0]);
            return projects;
        }

        /**
         * Sends the downloaded project list to the settings activity
         *
         * @param projects project list
         */
        @Override
        protected void onPostExecute(List<String> projects) {
            settingsActivity.projectsLoaded(projects, selectedProject);
        }
    }
}
