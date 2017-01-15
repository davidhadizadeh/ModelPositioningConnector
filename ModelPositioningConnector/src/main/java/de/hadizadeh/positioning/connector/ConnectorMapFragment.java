package de.hadizadeh.positioning.connector;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import de.hadizadeh.positioning.controller.MappedPositionManager;
import de.hadizadeh.positioning.controller.Technology;
import de.hadizadeh.positioning.model.MappingPoint;
import de.hadizadeh.positioning.model.SignalInformation;
import de.hadizadeh.positioning.roommodel.Map;
import de.hadizadeh.positioning.roommodel.android.MapFragment;
import de.hadizadeh.positioning.roommodel.android.ViewerMap;
import de.hadizadeh.positioning.roommodel.android.ViewerMapSegment;
import de.hadizadeh.positioning.roommodel.android.technologies.BluetoothLeDevice;
import de.hadizadeh.positioning.roommodel.android.technologies.BluetoothLeProximityTechnology;
import de.hadizadeh.positioning.roommodel.model.MapSegment;

import java.util.List;

/**
 * Manages a map for displaying room models and connecting them with fingerprints
 */
public class ConnectorMapFragment extends MapFragment {
    protected View popupView;
    protected PopupWindow popupInformation;
    private Button mapBtn;
    private Button removeBtn;
    private boolean popUpVisible;
    private TextView signalDataTv;
    private MappedPositionManager mappedPositionManager;
    private ReadMapDataHandler readMapDataHandler;

    /**
     * Sets necessary data for initialization
     *
     * @param viewerMap             map where the room model will be shown
     * @param floorText             text for selecting the floor
     * @param mappedPositionManager position manager for mapping the fingerprints
     */
    public void setData(ViewerMap viewerMap, String floorText, MappedPositionManager mappedPositionManager) {
        super.setData(viewerMap, floorText);
        this.mappedPositionManager = mappedPositionManager;
    }

    /**
     * Creates the map view
     *
     * @param inflater           layout inflater
     * @param container          layout container
     * @param savedInstanceState saved instance state
     * @return created view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.setLayoutIds(R.layout.fragment_map, R.id.fragment_map_canvas_box, R.id.fragment_map_floor_sp);
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mapBtn = (Button) view.findViewById(R.id.fragment_map_map_btn);
        removeBtn = (Button) view.findViewById(R.id.fragment_map_remove_btn);
        mapBtn.setEnabled(false);
        removeBtn.setEnabled(false);
        readMapDataHandler = new ReadMapDataHandler(this);


        if (savedInstanceState != null) {
            popUpVisible = savedInstanceState.getBoolean(getString(R.string.extra_popup_visible));
        } else {
            popUpVisible = true;
        }

        initPopupInformation();

        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MappingPoint mappingPoint = getSelectedMappingPoint();
                mappedPositionManager.map(mappingPoint);
                final String mappingPointName = MappedPositionManager.mappingPointToName(mappingPoint);
                new Thread() {
                    public void run() {
                        java.util.Map<String, SignalInformation> signalData = null;
                        for (int i = 0; i < 10 && (signalData == null || signalData.size() == 0); i++) {
                            List<Technology> technologies = mappedPositionManager.getTechnologies();
                            if (technologies != null && technologies.size() > 0) {
                                signalData = mappedPositionManager.getSignalInformation(mappingPointName, technologies.get(0).getName());
                            }
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                            }
                        }
                        if (signalData != null && signalData.size() > 0) {
                            Message message = readMapDataHandler.obtainMessage();
                            message.obj = new Object[]{mappingPoint, signalData};
                            readMapDataHandler.sendMessage(message);
                        }
                    }
                }.start();
            }
        });

        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MappingPoint mappingPoint = getSelectedMappingPoint();
                mappedPositionManager.removeMappedPosition(getSelectedMappingPoint());
                viewerMap.unmap(mappingPoint);
                viewerMap.render();
                canvasBox.invalidate();
                removeBtn.setEnabled(false);
                if (signalDataTv != null) {
                    signalDataTv.setText("-");
                }
            }
        });
        return view;
    }

    /**
     * Saves the current state of the popup with mapping data
     *
     * @param outState out state
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (popupInformation != null) {
            outState.putBoolean(getString(R.string.extra_popup_visible), popupInformation.isShowing());
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Dismisses the popup box when the fragment stops
     */
    @Override
    public void onStop() {
        if (popupInformation != null) {
            popupInformation.dismiss();
        }
        super.onStop();
    }

    @Override
    public void onTouchViewChange(float x, float y, float scale) {
        super.onTouchViewChange(x, y, scale);
        if (popupInformation != null) {
            popupInformation.dismiss();
        }
    }

    /**
     * Handles touch events of the room model map for zooming, scrolling and showing popup information
     *
     * @param x selected x coordinate
     * @param y selected y coordinate
     */
    @Override
    public void onTouch(float x, float y) {
        super.onTouch(x, y);
        if (x >= 0 && y >= 0 && touchedRow >= 0 && touchedColumn >= 0) {
            boolean mapped = viewerMap.onTouch(x, y).isMapped();
            mapBtn.setEnabled(true);
            removeBtn.setEnabled(mapped);
            if (popUpVisible) {
                showInformationPopup(x, y);
            }
            popUpVisible = true;
            viewerMap.onTouch(x, y);
        } else {
            if (popupInformation != null) {
                popupInformation.dismiss();
            }
            mapBtn.setEnabled(false);
            removeBtn.setEnabled(false);
            viewerMap.onTouch(-1, -1);
        }
        render();
    }

    /**
     * Initializes the popup information box
     */
    protected void initPopupInformation() {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        popupView = inflater.inflate(R.layout.information_popup, null, false);
        popupInformation = new PopupWindow(getActivity());
        popupInformation.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupInformation.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupInformation.setContentView(popupView);
    }

    /**
     * Shows a popup box at a defined position
     *
     * @param x starting x coordinate
     * @param y starting y coordinate
     */
    protected void showInformationPopup(float x, float y) {
        if (popupInformation != null) {
            popupInformation.dismiss();
        }
        int[] canvasBoxLocation = new int[2];
        canvasBox.getLocationInWindow(canvasBoxLocation);
        ViewerMapSegment currentMapSegment = (ViewerMapSegment) viewerMap.getMapSegments()[viewerMap.getCurrentFloor()][touchedRow][touchedColumn];

        float metersX = (touchedColumn + 1) / (float) Map.SEGMENTS_PER_METER;
        float metersY = (viewerMap.getRows() - (touchedRow)) / (float) Map.SEGMENTS_PER_METER;
        String titleText = "-";
        if (currentMapSegment.getContent() != null) {
            titleText = currentMapSegment.getContent().getTitle();
            if (titleText.length() > 16) {
                titleText = titleText.substring(0, 16) + "...";
            }

        }
        ((TextView) popupView.findViewById(R.id.information_popup_content_tv)).setText(titleText);
        String materialText = "-";
        if (currentMapSegment.getMaterial() != null) {
            materialText = (currentMapSegment.getMaterial().getPresentationName());
        }
        ((TextView) popupView.findViewById(R.id.information_popup_material_tv)).setText(materialText);

        ((TextView) popupView.findViewById(R.id.information_popup_x_tv)).setText(metersX + " " + getString(R.string.meters));
        ((TextView) popupView.findViewById(R.id.information_popup_y_tv)).setText(metersY + " " + getString(R.string.meters));
        signalDataTv = ((TextView) popupView.findViewById(R.id.information_popup_signal_tv));


        if (currentMapSegment.isMapped()) {
            java.util.Map<String, SignalInformation> signalData = null;
            List<Technology> technologies = mappedPositionManager.getTechnologies();
            if (technologies != null && technologies.size() > 0) {
                signalData = mappedPositionManager.getSignalInformation(
                        MappedPositionManager.mappingPointToName(currentMapSegment.getMappingPoint()), technologies.get(0).getName());
            }
            setSignalData(signalData);
        } else {
            signalDataTv.setText("-");
        }
        popupInformation.showAtLocation(canvasBox, Gravity.NO_GRAVITY, (int) (canvasBoxLocation[0] + x + MapSegment.getSize()), (int) (canvasBoxLocation[1] + y + +MapSegment.getSize()));
    }


    private void setSignalData(java.util.Map<String, SignalInformation> signalData) {
        if (signalDataTv != null && signalData != null) {
            if (signalData.entrySet().iterator().hasNext()) {
                String signalDataText = "";
                for (java.util.Map.Entry<String, SignalInformation> signalElement : signalData.entrySet()) {
                    try {
                        String id = String.valueOf(BluetoothLeProximityTechnology.idToNumber(signalElement.getKey()));
                        String value = getString(R.string.proximity_category_unknown);
                        if (signalElement.getValue().getStrength() == BluetoothLeDevice.DistanceCategory.IMMEDIATE.getValue()) {
                            value = getString(R.string.proximity_category_immediate);
                        } else if (signalElement.getValue().getStrength() == BluetoothLeDevice.DistanceCategory.NEAR.getValue()) {
                            value = getString(R.string.proximity_category_near);
                        } else if (signalElement.getValue().getStrength() == BluetoothLeDevice.DistanceCategory.FAR.getValue()) {
                            value = getString(R.string.proximity_category_far);
                        }
                        if (!"".equals(signalDataText)) {
                            signalDataText += "\n";
                        }
                        //signalDataText += "ID-" + id + ": " + value;
                        signalDataText += "ID-17: IMMEDIATE";
                    } catch (Exception ex) {
                        // it is not a BTLE technology in the correct format, so keep the unformatted id
                    }
                }
                signalDataTv.setText(signalDataText);
            }
        }
    }

    private void readMapData(MappingPoint mappingPoint, java.util.Map<String, SignalInformation> signalData) {
        viewerMap.map(mappingPoint);
        viewerMap.render();
        canvasBox.invalidate();
        removeBtn.setEnabled(true);
        setSignalData(signalData);
    }

    /**
     * Reads the current saved fingerprint after creating it to show it in the information popup box
     */
    static class ReadMapDataHandler extends Handler {
        private final ConnectorMapFragment activity;

        ReadMapDataHandler(ConnectorMapFragment activity) {
            this.activity = activity;
        }

        /**
         * Receives the the mapped fingerprint and sends it to the main activity
         *
         * @param msg current message
         */
        @Override
        public void handleMessage(Message msg) {
            Object[] data = (Object[]) msg.obj;
            MappingPoint mappingPoint = (MappingPoint) data[0];
            java.util.Map<String, SignalInformation> signalData = (java.util.Map<String, SignalInformation>) data[1];
            activity.readMapData(mappingPoint, signalData);
        }
    }
}
