package de.hadizadeh.positioning.connector;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.hadizadeh.positioning.roommodel.android.*;
import de.hadizadeh.positioning.roommodel.android.technologies.BluetoothLeProximityTechnology;
import de.hadizadeh.positioning.roommodel.android.technologies.BluetoothLeTechnology;
import de.hadizadeh.positioning.roommodel.android.technologies.CompassTechnology;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the main menu and the map for connecting fingerprints with the room model
 */
public class MainActivity extends PositioningActivity {
    private ListView menuLv;
    private ArrayAdapter<MenuItem> menuAdapter;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private boolean drawerOpen;
    private boolean replaceFragment;

    /**
     * Creates the view and initializes the needed technologies
     *
     * @param savedInstanceState saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String[] materialNames = getResources().getStringArray(R.array.material_names);
        setData(getString(R.string.settings_main_data_path), materialNames, R.id.activity_main_fragment_fl,
                getString(R.string.opening_data), getString(R.string.enable_location_service),
                getString(R.string.enable_location_service_yes), getString(R.string.enable_location_service_no),
                getString(R.string.confirmation), getString(R.string.downloading_data), getString(R.string.question_confirm_download),
                getString(R.string.uploading_data), getString(R.string.question_confirm_upload));

        addTechnology(new CompassTechnology(this, "COMPASS", 80));

        BluetoothLeTechnology btleTechnology;

        //btleTechnology = new BluetoothLeStrengthTechnology("BLUETOOTH_LE_STRENGTH", 3000, null);
        btleTechnology = new BluetoothLeProximityTechnology("BLUETOOTH_LE_PROXIMITY", new ArrayList<String>(), 3000);
        //btleTechnology = new BluetoothLeProximityCategoryTechnology("BLUETOOTH_LE_PROXIMITY_CATEGORY", new ArrayList<String>(), 3000);
        //btleTechnology.setMatcher(new OrderMatcher());

        addTechnology(btleTechnology);

        SharedPreferences sharedPref = getSharedPreferences(Settings.PREFERENCE_FILE, Context.MODE_PRIVATE);
        if (sharedPref.getString(Settings.WEBSERVICE, null) == null) {
            this.loadDataOnStartUp = false;
            startActivityForResult(new Intent(this, SettingsActivity.class), SettingsActivity.ACTIVITY_REQUEST_CODE);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<MenuItem> menuItems = new ArrayList<MenuItem>();
        menuItems.add(new MenuItem(this, getString(R.string.menu_settings), R.drawable.ic_action_settings));
        menuItems.add(new MenuItem(this, getString(R.string.menu_remove_positions), R.drawable.ic_action_discard));
        menuItems.add(new MenuItem(this, getString(R.string.menu_upload), R.drawable.ic_action_upload));
        menuItems.add(new MenuItem(this, getString(R.string.menu_download), R.drawable.ic_action_download));

        if (savedInstanceState == null) {
            replaceFragment = true;
        } else {
            Fragment currentFragment = fragmentManager.findFragmentById(layoutIdfragmentBox);
            if (currentFragment instanceof ConnectorMapFragment) {
                ConnectorMapFragment mapFragment = (ConnectorMapFragment) currentFragment;
                mapFragment.setData(viewerMap, getString(R.string.floor), mappedPositionManager);
            } else if (currentFragment instanceof LoadingFragment) {
                replaceFragment = true;
            }
        }

        menuLv = (ListView) findViewById(R.id.activity_main_menu_lv);
        menuAdapter = new MenuListAdapter(this, R.layout.menu_item, menuItems.toArray(new MenuItem[menuItems.size()]), R.id.menu_item_tv, R.id.menu_item_iv);
        menuLv.setAdapter(menuAdapter);
        menuLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), SettingsActivity.ACTIVITY_REQUEST_CODE);
                    drawerLayout.closeDrawers();
                } else if (position == 1) {
                    confirmUser(getString(R.string.confirmation), getString(R.string.question_confirm_remove), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mappedPositionManager.removeAllMappedPositions();
                            viewerMap.unmapAll();
                            ConnectorMapFragment mapFragment = getMapFragment();
                            if (mapFragment != null) {
                                mapFragment.render();
                            }
                        }
                    });
                } else if (position == 2) {
                    uploadPositioningFile();
//                    confirmUser(getString(R.string.confirmation), getString(R.string.question_confirm_upload), new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int whichButton) {
//                            uploadPositioningFile();
//                        }
//                    });
                } else if (position == 3) {
                    confirmUser(getString(R.string.confirmation), getString(R.string.question_confirm_download), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            downloadPositioningFile();
                            downloadMefData();
                        }
                    });
                }
            }
        });

        addMenu();
    }

    /**
     * Closes the menu drawer if it is open, else closes the application
     */
    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Called, when the settings activity is closed to reload the data, if the server or project information have changed
     *
     * @param requestCode request code
     * @param resultCode  result code
     * @param data        data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SettingsActivity.ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                replaceFragment = true;
                loadSettings();
                downloadMefData();
            }
        }
    }

    private ConnectorMapFragment getMapFragment() {
        Fragment currentFragment = fragmentManager.findFragmentById(layoutIdfragmentBox);
        if (currentFragment instanceof ConnectorMapFragment) {
            return (ConnectorMapFragment) currentFragment;
        }
        return null;
    }

    private void addMenu() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_gray));
        }

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.activity_main_drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                ConnectorMapFragment mapFragment = getMapFragment();
                if (mapFragment != null) {
                    mapFragment.unselectMap();
                }
                drawerOpen = false;
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                ConnectorMapFragment mapFragment = getMapFragment();
                if (mapFragment != null) {
                    mapFragment.unselectMap();
                }
                drawerOpen = true;
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();
        drawerLayout.setEnabled(false);
    }

    /**
     * Called, when all room model data have been loaded, replaces the loading fragment with the map fragment
     */
    @Override
    protected void dataLoaded() {
        super.dataLoaded();
        ConnectorMapFragment mapFragment = new ConnectorMapFragment();
        mapFragment.setData(viewerMap, getString(R.string.floor), mappedPositionManager);
        if (replaceFragment) {
            replaceFragment(mapFragment);
        }
        mappedPositionManager.startPositioning(1000);
    }
}
