package com.mycompany.myfirstindoorsapp;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.customlbs.coordinates.GeoCoordinate;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.callbacks.LoadingBuildingStatus;
import com.customlbs.library.callbacks.RoutingCallback;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.Floor;
import com.customlbs.library.model.Zone;
import com.customlbs.shared.Coordinate;
import com.customlbs.surface.library.DefaultSurfacePainterConfiguration;
import com.customlbs.surface.library.IndoorsSurface;
import com.customlbs.surface.library.IndoorsSurfaceFactory;
import com.customlbs.surface.library.IndoorsSurfaceFragment;
import com.customlbs.surface.library.SurfacePainterConfiguration;
import com.customlbs.surface.library.SurfaceState;
import com.customlbs.surface.library.ViewMode;

// beacon handling
//import org.altbeacon.beacon.Beacon;
//import org.altbeacon.beacon.BeaconConsumer;
//import org.altbeacon.beacon.BeaconManager;
//import org.altbeacon.beacon.BeaconParser;
//import org.altbeacon.beacon.RangeNotifier;
//import org.altbeacon.beacon.Region;

/**
 * Sample Android project, powered by indoo.rs :)
 *
 * @author indoo.rs | Philipp Koenig
 *
 */

// May want to redisgn using Material design
// http://alexzh.com/tutorials/material-style-for-dialogs-in-android-application/
// https://my.indoo.rs/javadoc/mmt_guide/#MeasurePoints
// JAVADOC : https://my.indoo.rs/javadoc/

public class MainActivity
    extends FragmentActivity
    implements
        IndoorsLocationListener,
        TextToSpeech.OnInitListener
//        BeaconConsumer
{

	final Context context = this;

	protected static final int REQUEST_OK = 1;
	protected static final String
		VOICE_TAG = "Voice",
		LOG_TAG = "DEBUG: ",
		BEACON_SIG_TAG = "BEACON_SIG_TEST: ";

	// UI Elements
	private IndoorsSurfaceFragment indoorsFragment;
	//private ZoneSelectionFragment zoneSelectionFragment;
	private SurfaceState surfaceState;
	private IndoorsSurface indoorsSurface;

	private Menu menu;
	private EditText startText, endText;
	private Button cancel, display;

	private Building building;
    private long buildingId = 566904373; // old 579366046 ( 579387616 = expert mode version )
    private boolean inRoute = false;
	public List<Zone> currZones;

	private TextToSpeech tts;
//	private BeaconManager beaconManager;

	private ArrayList<Coordinate> activeRouteCoordinates = new ArrayList<Coordinate>();
	private Coordinate start1, end1, activeDestination = null;


    // Things to Update later:
    // when closing and coming back to app
        // save settings like:
            // show all zones
            // number of steps?

    //look up route snapping


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.layout);
		super.onCreate(savedInstanceState);
		IndoorsFactory.Builder indoorsBuilder = new IndoorsFactory.Builder();
		IndoorsSurfaceFactory.Builder surfaceBuilder = new IndoorsSurfaceFactory.Builder();

		indoorsBuilder.setContext(this);
		indoorsBuilder.setApiKey("fc6d16ff-e3b1-4b2a-b43e-d09c5f152063");

		// our cloud using the MMT Tool
		indoorsBuilder.setBuildingId(buildingId);
		indoorsBuilder.setEvaluationMode(false);


		// callback for indoo.rs-events
		indoorsBuilder.setUserInteractionListener(this);

		surfaceBuilder.setIndoorsBuilder(indoorsBuilder);

		// navigation arrow
		Bitmap navigationArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow);
		SurfacePainterConfiguration configuration = DefaultSurfacePainterConfiguration.getConfiguration();
		configuration.setNavigationArrow(navigationArrow);

		surfaceBuilder.setSurfacePainterConfiguration(configuration);

		indoorsFragment = surfaceBuilder.build();
		indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_CURRENT_ZONE);
		indoorsFragment.getSurfaceState().orientedNaviArrow = true; // set navigation arrow on
//		indoorsFragment.registerOnSurfaceClickListener(surfaceClicked);

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(android.R.id.content, indoorsFragment, "indoors");
		transaction.commit();

		tts = new TextToSpeech(this, this);
//		initBeaconFinder();
		/*
		if (getResources().getBoolean(R.bool.default_show_all_zones)) {
			indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_ALL_ZONES);
		}

		if (getResources().getBoolean(R.bool.default_zone_fencing_enabled)) {
			indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_CURRENT_ZONE);
		}
		*/

	}
    /*
	public void initBeaconFinder(){
		beaconManager = BeaconManager.getInstanceForApplication(this);
		beaconManager.getBeaconParsers().add(new BeaconParser().
				setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
		beaconManager.bind(this);
	}
	*/

	public void surfaceClicked(IndoorsSurface.OnSurfaceClickListener mListener){
		Toast.makeText(this, "clicked map", Toast.LENGTH_LONG).show();
	}

	private void speakOut(String txt) {
		tts.speak(txt, TextToSpeech.QUEUE_FLUSH, null);
	}

	public void routeAToB(Coordinate destCoord){
		indoorsFragment.getIndoors().getRouteAToB(indoorsFragment.getCurrentUserPosition(), destCoord, new RoutingCallback() {

			@Override
			public void onError(IndoorsException arg0) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG, "Routing error" + arg0.toString());
			}

			@Override
			public void setRoute(ArrayList<Coordinate> arg0) {
				Log.d(LOG_TAG, "Coords: " + arg0.get(arg0.size() - 1).x + ", " + arg0.get(arg0.size() - 1).y);
				Toast.makeText(context, "setRoute" + arg0.get(0).x + ", " + arg0.get(0).y, Toast.LENGTH_LONG).show();

				indoorsFragment.getSurfaceState().setRoutingPath(arg0, false);
				indoorsFragment.updateSurface();

				activeRouteCoordinates = (ArrayList<Coordinate>) arg0.clone();
				inRoute = true;

				if (activeRouteCoordinates.size() > 0) {
					String turnDir = determineNextTurn(arg0.get(0));
					if (turnDir.equals("")) {
						speakOut("Go Straight");
					} else {
						speakOut("Turn " + turnDir);
					}

				}
			}
		});
	}


	public String determineNextTurn(Coordinate userPosition){
		String turnDirection = "";
		if(activeRouteCoordinates.size() > 2) {

			// 3 points ( userPosition, activeRouteCoordinates[1], activeRouteCoordinates[2] )
			// whats the diff between userPos and activeRouteCoords[1]
			// check which diff is greater
			String diffVal1 = getGreaterDiff(userPosition, activeRouteCoordinates.get(1));
			//        CoordType type = CoordType.valueOf(diffVal1);

			String diffVal2 = getGreaterDiff(activeRouteCoordinates.get(1), activeRouteCoordinates.get(2));
			//        CoordType type2 = CoordType.valueOf(diffVal2);

			// first hop
			/*
			switch(type){
				case XP: // x pos
					if(diffVal2.equals("YP")){
						turnDirection = "Right";
					}else if(diffVal2.equals("YN")){
						turnDirection = "Left";
					}
					break;
				case XN: // x neg
					if(diffVal2.equals("YP")){
						turnDirection = "Left";
					}else if(diffVal2.equals("YN")){
						turnDirection = "Right";
					}
					break;
				case YP: // y pos
					if(diffVal2.equals("XP")){
						turnDirection = "Left";
					}else if(diffVal2.equals("XN")){
						turnDirection = "Right";
					}
					break;
				case YN: // y neg
					if(diffVal2.equals("XP")){
						turnDirection = "Right";
					}else if(diffVal2.equals("XN")){
						turnDirection = "Left";
					}
					break;
			}
			*/

			if (diffVal1.equals("XP")) {
				if (diffVal2.equals("YP")) {
					turnDirection = "Right";
				} else if (diffVal2.equals("YN")) {
					turnDirection = "Left";
				}
			}
			if (diffVal1.equals("XN")) {
				if (diffVal2.equals("YP")) {
					turnDirection = "Left";
				} else if (diffVal2.equals("YN")) {
					turnDirection = "Right";
				}
			}
			if (diffVal1.equals("YP")) {
				if (diffVal2.equals("XP")) {
					turnDirection = "Left";
				} else if (diffVal2.equals("XN")) {
					turnDirection = "Right";
				}
			}
			if (diffVal1.equals("YN")) {
				if (diffVal2.equals("XP")) {
					turnDirection = "Right";
				} else if (diffVal2.equals("XN")) {
					turnDirection = "Left";
				}
			}

			if(!turnDirection.equals("")) {
				Toast.makeText(this, "turn " + turnDirection, Toast.LENGTH_LONG).show();
			}
			activeRouteCoordinates.remove(0); // remove first coord in activeRouteCoordinates[]
		}
        return turnDirection;
	}

	// returns whichever difference is greater
    // may want to return hashmap - or string with ( x or y )
	public String getGreaterDiff(Coordinate coord1, Coordinate coord2){
		String diffVal;
        int x1 = coord1.x,
			x2 = coord2.x,
			y1 = coord1.y,
			y2 = coord2.y;

		// difference between x's is greater
		if( Math.abs(x2 - x1) > Math.abs(y2 - y1) ){
			Log.d(LOG_TAG, "DIFF" + Math.abs(x2 - x1) + ":"+ Math.abs(y2 - y1));
            diffVal = "X";
            diffVal += ((x2 - x1) > 0) ? "P" : "N";// may want to change to string ( pos or neg )
		}
		// difference between y's is greater
		else{
            diffVal = "Y";
            diffVal += ((y2 - y1) > 0) ? "P" : "N";
		}
		Log.d(LOG_TAG, "diffVal: " + diffVal);
		return diffVal;
	}

	// need to use this to update view ( need accurate res )
	public void positionUpdated(Coordinate userPosition, int accuracy) {

		// REROUTING - OFF COURSE
		// save current destination as class variable
		// save array of zones that exist in a given route
		// check if current zone they are at is in the array  of zones in that route
		// if so then they are good, if not recalculate to their destination give their current location

        // make sure they are in a route
        if(inRoute){
            // if the current route coordinates array has entries
            if(activeRouteCoordinates.size() > 0) {
                String turnDir = determineNextTurn(userPosition);
                if(turnDir.equals("")){
                    speakOut("Go Straight");
                }else{
                    speakOut("Turn " + turnDir);
                }
            }

            // handle rerouting ( NEEDS TO BE FINISHED )
            if(activeDestination != null) {
//                routeAToB(activeDestination);
            }
        }
	}


	@Override
	public void loadingBuilding(LoadingBuildingStatus loadingBuildingStatus) {
		int progress = loadingBuildingStatus.getProgress();
	}

	public void buildingLoaded(Building building) {
		// indoo.rs SDK successfully loaded the building you requested and
		// calculates a position now
        Log.d(LOG_TAG, "buildingLoaded");
		Toast.makeText(this,
				"Building is located at " + building.getLatOrigin() / 1E6 + ","
						+ building.getLonOrigin() / 1E6, Toast.LENGTH_SHORT).show();
	}

	public void onError(IndoorsException indoorsException) {
		Toast.makeText(this, indoorsException.getMessage(), Toast.LENGTH_LONG).show();
	}

	public void changedFloor(int floorLevel, String name) {
		// user changed the floor
		try {
			if (floorLevel != Integer.MAX_VALUE) { // nur wenn auch definitiv sicher
				if (name.equals("floor")) { // Name Hack (indoo.rs SDK <= 1.9.0 floor name is always "floor")
					Floor floor = building.getFloorByLevel(floorLevel);
					name = floor.getName();
				}
				//sendResult("changedFloor", floorLevel + "|" + name, "message", PluginResult.Status.OK);
			}
		} catch (Exception e) {
			Log.d(LOG_TAG, "ChangedFloor Exception: " + e.toString());
		}
	}

	public void leftBuilding(Building building) {
		// user left the building
	}

	public void loadingBuilding(int progress) {
		// indoo.rs is still downloading or parsing the requested building
	}

	public void orientationUpdated(float orientation) {
		// user changed the direction he's heading to
//		Toast.makeText(this,"User Direction " + orientation, Toast.LENGTH_SHORT).show();
//		Log.d(LOG_TAG, "orientation: " + orientation);
	}

	public void enteredZones(List<Zone> zones) {
		for (Zone zone : zones) {
			Toast.makeText(this, "You are at the Zone " + zone.getName(), Toast.LENGTH_SHORT).show();
		}
	}
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		this.menu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d(LOG_TAG, "Zone Name: ");
		int id = item.getItemId();
		switch (id){
			case R.id.route_search:
                stopCurrentRoute();
				final Dialog dialog = new Dialog(MainActivity.this);
				dialog.setContentView(R.layout.layout);
				dialog.setTitle("Where do you want to go ?");
				startText = ((EditText) dialog.findViewById(R.id.route_startText));
				endText = ((EditText) dialog.findViewById(R.id.route_endText));
				display = (Button) dialog.findViewById(R.id.route_startCalculation);
				cancel = (Button) dialog.findViewById(R.id.route_cancel);

				display.setOnClickListener(new View.OnClickListener(){

					@Override
					public void onClick(View v){
						dialog.dismiss();
						String[] coords = endText.getText().toString().split(",");
						int x = Integer.parseInt(coords[0]),
							y = Integer.parseInt(coords[1]),
							z = Integer.parseInt(coords[2]);
						Coordinate zoneCoord = new Coordinate(x,y,z);

						indoorsFragment.getIndoors().getRouteAToB(indoorsFragment.getCurrentUserPosition(), zoneCoord, new RoutingCallback() {

							@Override
							public void onError(IndoorsException arg0) {
								// TODO Auto-generated method stub
								Log.d(LOG_TAG, "Routing error" + arg0.toString());
							}

							@Override
							public void setRoute(ArrayList<Coordinate> arg0) {
								Toast.makeText(context, "setRoute" +  arg0.get(0).x + ", " + arg0.get(0).y, Toast.LENGTH_LONG).show();
								indoorsFragment.getSurfaceState().setRoutingPath(arg0, false);
								indoorsFragment.updateSurface();
							}
						});
					}
				});

				cancel.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.cancel();
					}
				});
				dialog.show();
				return true;

			case R.id.menu_speech_invoke:
                stopCurrentRoute();
				startSpeechRecognizer();
				return true;
			case R.id.menu_search_invoke:
                stopCurrentRoute();
				showRoutesDialog();
				return true;
			case R.id.menu_show_all_zones:
				item.setChecked(!item.isChecked());
				refreshViewMode();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

    private void stopCurrentRoute(){
        indoorsFragment.updateSurface();
        inRoute = false;
    }

    private void showRoutesDialog(){
        // show all zones here and add on click listener to each to calc route
        final Dialog searchDialog = new Dialog(MainActivity.this);
        searchDialog.setContentView(R.layout.search_routes);
        searchDialog.setTitle("Select a destination");

        TableLayout tblLayout = (TableLayout)searchDialog.findViewById(R.id.tblSearchRoutes);

        cancel = (Button) searchDialog.findViewById(R.id.search_cancel);

        indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_ALL_ZONES);
        currZones = indoorsFragment.getZones(); // gets all zones

        for(Zone zone : currZones){
            Log.d(LOG_TAG, "Zone Name: " + zone.getName());
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            View spacer = new View(this);
            spacer.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));
            spacer.setBackgroundColor(Color.WHITE);

            TextView t = new TextView(this);
            t.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            int tenDp = getPixelsByDp(10);
            t.setPadding(tenDp, tenDp, tenDp, tenDp);

            // get first coordinate in arraylist for given zone
            Coordinate zoneCoord = ((ArrayList < Coordinate >)zone.getZonePoints()).get(2);
            String zoneCoordString = zoneCoord.x + "," + zoneCoord.y + "," + zoneCoord.z;
            t.setContentDescription( zoneCoordString );

            t.setText(zone.getName()); // sets the text view to name of zone

            t.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    String zoneCoords = v.getContentDescription().toString();
                    String[] coords = zoneCoords.split(",");
                    int x = Integer.parseInt(coords[0]),
                            y = Integer.parseInt(coords[1]),
                            z = Integer.parseInt(coords[2]);
                    Coordinate zoneCoord = new Coordinate(x, y, z);
                    activeDestination = zoneCoord;
                    searchDialog.dismiss();

                    routeAToB(activeDestination);
                }
            });
            // set onclick listener
            row.addView(t);
            row.addView(spacer);
            tblLayout.addView(row);
        }

        cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchDialog.cancel();
			}
		});

        searchDialog.show();
    }

    private void startSpeechRecognizer(){
        // set up speech recognizer
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        try {
            startActivityForResult(i, REQUEST_OK);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing speech to text engine:" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

	private void refreshViewMode() {
		indoorsFragment.setViewMode(ViewMode.DEFAULT);

		MenuItem allZonesItem = menu.findItem(R.id.menu_show_all_zones);
		if (allZonesItem.isChecked()) {
			indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_ALL_ZONES);
		}
	}

	public int getPixelsByDp(int sizeInDp){
		float scale = getResources().getDisplayMetrics().density;
		return (int) (sizeInDp*scale + 0.5f);
	}
    /*
	public Beacon findActiveBeacon(Beacon nearbyBeacon){

		Beacon currMapBeacon;

		// search through beacons on map and find the one that we are closest to.



		return currMapBeacon;
	}
	*/

	/*
	 * Handles searching for beacons nearby and
	 * determining the distance from your current location to the beacon
	 */

    /*
	@Override
	public void onBeaconServiceConnect() {
		beaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
				Log.d(BEACON_SIG_TAG, "Beacon Array Size: " + beacons.size());
				if (beacons.size() > 0) {
					Beacon nearbyBeacon = beacons.iterator().next();
					Log.i(BEACON_SIG_TAG, "The first beacon I see is about " + nearbyBeacon.getDistance() + " meters away.");

					// What other properties do Beacons have?
					// try to match that beacon with the beacons saved in the map

					// once we know which beacon it is
						// find out which beacon / coordinate is next in activeRouteCoords
						// then call determine next turn??

					findActiveBeacon(nearbyBeacon);


				}
			}
		});

		try {
			Log.d(BEACON_SIG_TAG, "Ranging: ");
			beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
		} catch (RemoteException e) {  Log.d(BEACON_SIG_TAG, "ranging exception: " + e.toString());  }
	}
	*/

	/*
	 * Handles results from speech to text listener
	 */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==REQUEST_OK  && resultCode==RESULT_OK) {
			ArrayList<String> thingsYouSaid = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			String whole =  "";

			for(String thing : thingsYouSaid){
				whole += " " + thing;
			}
			whole = whole.substring(1); //remove first space
			Log.d(VOICE_TAG, whole);

			currZones = indoorsFragment.getZones(); // gets all zones
			String retVal = "You did not say an existing location.";


			for(Zone zone : currZones) {
				String zoneName = zone.getName();
				Coordinate zoneCoord = zone.getZonePoints().get(2);
				if(thingsYouSaid.get(0).toLowerCase().equals("route me to " + zoneName.toLowerCase()) ){
					Log.d(VOICE_TAG, "YOU SAID ROUTE ME TO " + zoneName);
					retVal = "Routing to " + zoneName;
					activeDestination = zoneCoord;
					routeAToB(activeDestination);

				}else if(thingsYouSaid.get(0).toLowerCase().equals("route me to class " + zoneName.toLowerCase()) ){
					Log.d(VOICE_TAG, "YOU SAID ROUTE ME TO CLASS " + zoneName);
					retVal = "Routing to " + zoneName;
					activeDestination = zoneCoord;
					routeAToB(activeDestination);

				}
			}
			Toast.makeText(this, retVal, Toast.LENGTH_LONG).show();
			speakOut(retVal);
		}
	}

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = tts.setLanguage(Locale.US);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("TTS", "This Language is not supported");
			}
		} else {
			Log.e("TTS", "Initilization Failed!");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		beaconManager.unbind(this);
	}

	@Override
	protected void onStop() {
		super.onStop();

		// Don't forget to shutdown tts!
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}

//		indoors.removeLocationListener(this);
//		IndoorsFactory.releaseInstance(this);
	}

}

//grep -rnw * -e "menu_search_invoke"
//RoutingViewFragment.java