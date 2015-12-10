package com.mycompany.myfirstindoorsapp;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

/**
 * Sample Android project, powered by indoo.rs :)
 *
 * @author indoo.rs | Philipp Koenig
 *
 */

// May want to redesign using Material design
// http://alexzh.com/tutorials/material-style-for-dialogs-in-android-application/
// https://my.indoo.rs/javadoc/mmt_guide/#MeasurePoints
// JAVADOC : https://my.indoo.rs/javadoc/

/**
 * This class is the main activity for the app
 *
 * It extends FragmentActivity because the Indoo.rs API requires it to inorder to handle
 * the IndoorsSurfaceFragment that displays the map and handles the map interaction
 *
 * It implements the IndoorLocationListener interface because the app needs to listen
 * for location updates via beacon proximity
 *
 * It implements the TextToSpeech.OnInitListener interface because the app needs to handle
 * voice commands because our user base consists of people who are blind
 *
 * It implements the BeaconConsumer interface because the app needs to scan for
 * nearby beacons and find the closest beacon
 */

public class MainActivity
    extends FragmentActivity
    implements
        IndoorsLocationListener,
        TextToSpeech.OnInitListener,
        BeaconConsumer
{

	final Context context = this;

	protected static final int REQUEST_OK = 1;

    // Debug Log Tags
	protected static final String
		VOICE_TAG = "Voice",
		LOG_TAG = "DEBUG: ",
		BEACON_SIG_TAG = "BEACON_SIG_TEST: ";

	// UI Related Variables
	private IndoorsSurfaceFragment indoorsFragment;
	//private ZoneSelectionFragment zoneSelectionFragment;
	private SurfaceState surfaceState;
	private IndoorsSurface indoorsSurface;

    // UI Elements
	private Menu menu;
	private EditText startText, endText;
	private Button cancel, display;

    // Building variables
	private Building building;
	/********
	 * CHANGE THIS BUILDING ID TO THE ID OF WHICHEVER BUILDING YOU ARE USING!
	 * *********/

    // ID with major-minor zone descriptions = 566904373
    private long buildingId = 594812644; // prev - 566904373 // old 579366046 ( 579387616 = expert mode version )

    // Routing related variables
    private boolean inRoute = false;
	public List<Zone> currZones = new ArrayList<Zone>();

	private TextToSpeech tts; // class that handles speaking out text
	private BeaconManager beaconManager; // class that handles scanning for beacons

	private ArrayList<Coordinate> activeRouteCoordinates = new ArrayList<Coordinate>(); // the list of coordinates for the currently displayed route
	private Coordinate
            start1,
            end1,
            activeDestination = null; // the coordinate for the current destination


	private double  nearestBeaconDist = 0,
                    totalDistUntilTurn = 0,
                    distToNextRouteCoord = 0;

	private HashMap<String, Zone> currZonesMap = new HashMap<String, Zone>();

    // Things to Update later:
    // when closing and coming back to app
        // save settings like:
            // show all zones
            // number of steps?

    /**
     * This method fires when the app opens
     * @param savedInstanceState
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.layout);
		super.onCreate(savedInstanceState);
		IndoorsFactory.Builder indoorsBuilder = new IndoorsFactory.Builder();
		IndoorsSurfaceFactory.Builder surfaceBuilder = new IndoorsSurfaceFactory.Builder();

		indoorsBuilder.setContext(this);
        // **** CHANGE THIS API KEY TO THE ONE IN YOUR ACCOUNT INFO PAGE @ https://my.indoo.rs/#  ****//
		indoorsBuilder.setApiKey("fc6d16ff-e3b1-4b2a-b43e-d09c5f152063");

		// our cloud using the MMT Tool
		indoorsBuilder.setBuildingId(buildingId);
		indoorsBuilder.setEvaluationMode(false);

		// callback for indoo.rs-events
		indoorsBuilder.setUserInteractionListener(this);

        // sets the UI builder that handles the map UI
		surfaceBuilder.setIndoorsBuilder(indoorsBuilder);

		// set navigation arrow for current user position
		Bitmap navigationArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow);
		SurfacePainterConfiguration configuration = DefaultSurfacePainterConfiguration.getConfiguration();
		configuration.setNavigationArrow(navigationArrow);

		surfaceBuilder.setSurfacePainterConfiguration(configuration);

        // build the Indoor fragment
		indoorsFragment = surfaceBuilder.build();
		indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_CURRENT_ZONE);
		indoorsFragment.getSurfaceState().orientedNaviArrow = true; // set navigation arrow on
//		indoorsFragment.registerOnSurfaceClickListener(surfaceClicked);

        // set current fragment to "indoors" so map displays
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(android.R.id.content, indoorsFragment, "indoors");
		transaction.commit();

		tts = new TextToSpeech(this, this); // initialize the textToSpeech engine
		initBeaconFinder(); // initialize the Beacon scanning

		/*
		if (getResources().getBoolean(R.bool.default_show_all_zones)) {
			indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_ALL_ZONES);
		}

		if (getResources().getBoolean(R.bool.default_zone_fencing_enabled)) {
			indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_CURRENT_ZONE);
		}
		*/

	}

    /**
     * Initializes the Beacon scanner with the proper ID type for our Kontakt Beacons
     */
	public void initBeaconFinder(){
		beaconManager = BeaconManager.getInstanceForApplication(this);
		beaconManager.getBeaconParsers().add(new BeaconParser().
				setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")); // Kontakt Beacon ID type
		beaconManager.bind(this);
	}

    /**
     * We wanted this to create a route to the clicked location. ( this would not be useful for blind users though )
     * @param mListener
     */
	public void surfaceClicked(IndoorsSurface.OnSurfaceClickListener mListener){
		Toast.makeText(this, "clicked map", Toast.LENGTH_LONG).show();
	}

    /**
     * handles speaking out a given string
     * @param txt : a string you want the app to speak out to the user
     */
	private void speakOut(String txt) {
		tts.speak(txt, TextToSpeech.QUEUE_FLUSH, null);
	}

    /**
     * Handles routing from the users current position to a destination coordinate
     * @param destCoord : destination coordinate
     */
	public void routeToCoord(Coordinate destCoord){
        // make sure we can get their current location relative to the map
        if(indoorsFragment.getCurrentUserPosition() != null){
            indoorsFragment.getIndoors().getRouteAToB(indoorsFragment.getCurrentUserPosition(), destCoord, new RoutingCallback() {

                /**
                 * If error occrus then log it
                 * @param arg0
                 */
                @Override
                public void onError(IndoorsException arg0) {
                    // TODO Auto-generated method stub
                    Log.d(LOG_TAG, "Routing error" + arg0.toString());
                }

                /**
                 * handles setting the route and displaying the route on the map
                 * @param arg0 : the arraylist of route coordinates
                 */
                @Override
                public void setRoute(ArrayList<Coordinate> arg0) {
                    Log.d(LOG_TAG, "Coords: " + arg0.get(arg0.size() - 1).x + ", " + arg0.get(arg0.size() - 1).y);
                    Toast.makeText(context, "setRoute" + arg0.get(0).x + ", " + arg0.get(0).y, Toast.LENGTH_LONG).show();

                    indoorsFragment.getSurfaceState().setRoutingPath(arg0);
                    indoorsFragment.updateSurface();

                    activeRouteCoordinates = (ArrayList<Coordinate>) arg0.clone();
                    inRoute = true;

                    // if the route array is not empty then start turn by turn directions
                    if (activeRouteCoordinates.size() > 0) {
                        String turnDir = determineNextTurn(arg0.get(0));
                        // no turn necessary
                        if (turnDir.equals("")) {
                            speakOut("Go Straight for " +  Math.floor(distToNextRouteCoord) + " meters");
                        } else {
                            speakOut("Turn " + turnDir + " in " +  Math.floor(totalDistUntilTurn) + " meters");
                        }

                    }
                }
            });
        }else{
            Toast.makeText(context, "Sorry, you are not currently at this location", Toast.LENGTH_LONG).show();
        }
	}

    /**
     * handles determining turns ( needs to be refined to handle route tails )
     * @param userPosition : users current position on the map
     * @return
     */
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
            // for some reason cant use switch for this?
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

            // this could be improved by creating a hashmap : HashMap<String, HashMap<String, String>>()
            // and the finding the value surrounded by ** in HashMap<String, HashMap<String, **String**>>
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

            // making sure turnDirection has a value ( will be either right or left )
			if(!turnDirection.equals("")) {
				Toast.makeText(this, "turn " + turnDirection + " in " + Math.floor(totalDistUntilTurn) + " meters", Toast.LENGTH_LONG).show();
			}
			activeRouteCoordinates.remove(0); // remove first coord in activeRouteCoordinates[]
		}
        return turnDirection;
	}


    /**
     * returns whichever difference is greater
     * may want to return hashmap - or string with ( x or y )
     *
     * @param coord1
     * @param coord2
     * @return
     */
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

                // need to rerouote if they turn around

                // current distance needs to include distance to turn

                String turnDir = determineNextTurn(userPosition);
                if(turnDir.equals("")){
                    speakOut("Go Straight for " + Math.floor(distToNextRouteCoord) + " meters");
                }else{
                    speakOut("Turn " + turnDir + " in " +  Math.floor(totalDistUntilTurn) + " meters");
                }
            }

            // handle rerouting ( NEEDS TO BE FINISHED )
            if(activeDestination != null) {
//                routeToCoord(activeDestination);
            }
        }
	}


    public double getDistanceInMeters(Coordinate c1, Coordinate c2){
        double distance = 0;
        // get difference between coordinates
        if(c1 != null && c2 != null){
            int x1 = c1.x,
                    x2 = c2.x,
                    y1 = c1.y,
                    y2 = c2.y;

            // difference between x's is greater
            if( Math.abs(x2 - x1) > Math.abs(y2 - y1) ){
                Log.d(LOG_TAG, "DIFF" + Math.abs(x2 - x1) + ":"+ Math.abs(y2 - y1));

                distance = Math.abs(x2 - x1);
            }
            // difference between y's is greater
            else{
                distance = Math.abs(y2 - y1);
            }

            // convert to meters
            // ideally we would want to get the scale from the API instead of hardcoding it because
            double scale = .0004453; // meters to pixels

            distance = distance * scale;
        }

        return distance;
    }


    /**
     * get progress of building info loading ( Map and other data )
     *
     * @param loadingBuildingStatus
     */
	@Override
	public void loadingBuilding(LoadingBuildingStatus loadingBuildingStatus) {
		int progress = loadingBuildingStatus.getProgress();
	}

    /**
     * Fires when building is done loading
     * @param building
     */
	public void buildingLoaded(Building building) {
		// indoo.rs SDK successfully loaded the building you requested and
		// calculates a position now
        Log.d(LOG_TAG, "buildingLoaded");
		Toast.makeText(this,
				"Building is located at " + building.getLatOrigin() / 1E6 + ","
						+ building.getLonOrigin() / 1E6, Toast.LENGTH_SHORT).show();
	}

    /**
     * Error occurs
     * @param indoorsException
     */
	public void onError(IndoorsException indoorsException) {
		Toast.makeText(this, indoorsException.getMessage(), Toast.LENGTH_LONG).show();
	}

    /**
     * change to different floor in building
     * @param floorLevel
     * @param name
     */
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

    /**
     * When you enter a zone on the map
     * @param zones
     */
	public void enteredZones(List<Zone> zones) {
		for (Zone zone : zones) {
			Toast.makeText(this, "You are at the Zone " + zone.getName(), Toast.LENGTH_SHORT).show();
		}
	}

    /**
     * handles creating the app menu
     * @param menu
     * @return
     */
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		this.menu = menu;
		return super.onCreateOptionsMenu(menu);
	}

    /**
     * when an item in the menu is selected
     * @param item
     * @return
     */
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
								indoorsFragment.getSurfaceState().setRoutingPath(arg0);
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

    /**
     * Stops routing
     */
    private void stopCurrentRoute(){
        indoorsFragment.updateSurface();
        inRoute = false;
    }

    /**
     * Display a dialog with all zones ( room numbers )
     */
    private void showRoutesDialog(){
        // show all zones here and add on click listener to each to calc route
        final Dialog searchDialog = new Dialog(MainActivity.this);
        searchDialog.setContentView(R.layout.search_routes);
        searchDialog.setTitle("Select a destination");

        TableLayout tblLayout = (TableLayout)searchDialog.findViewById(R.id.tblSearchRoutes);

        cancel = (Button) searchDialog.findViewById(R.id.search_cancel);

        indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_ALL_ZONES);
        getAllZones();

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

                    routeToCoord(activeDestination);
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

    /**
     * Initialize the Speech Command Recognizer
     */
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

    /**
     * refreshes the view mode of the map.
     */
	private void refreshViewMode() {
		indoorsFragment.setViewMode(ViewMode.DEFAULT);

		MenuItem allZonesItem = menu.findItem(R.id.menu_show_all_zones);
		if (allZonesItem.isChecked()) {
			indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_ALL_ZONES);
		}
	}

    /**
     * Utility function to get the pixel equivalent of a size in "dp"
     * @param sizeInDp
     * @return
     */
	public int getPixelsByDp(int sizeInDp){
		float scale = getResources().getDisplayMetrics().density;
		return (int) (sizeInDp*scale + 0.5f);
	}


	/**
	 * Retrieves all zones registered in the map for use in the code
	 */
	private void getAllZones(){
		// may not need to make currZones a class variable
		if( currZones.size() == 0 ){
			currZones = indoorsFragment.getZones(); // gets all zones
		}

        // populate the HashMap with the zones
        // where the key is the zone name and the value is the zone
        // makes finding zones easier in code
        for(Zone zone : currZones) {
            if(zone.getDescription() != null){
                currZonesMap.put(zone.getDescription(), zone);
            }
        }
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
	@Override
	public void onBeaconServiceConnect() {
		beaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
				if (beacons.size() > 0) {
					Beacon nearbyBeacon = beacons.iterator().next();
					Log.i(BEACON_SIG_TAG, "The first beacon I see is about " + nearbyBeacon.getDistance() + " meters away.");
					Log.i(BEACON_SIG_TAG, "Beacon bl add " + nearbyBeacon.getBluetoothAddress());



                    Beacon secNearestBeacon = beacons.iterator().next();

                    getAllZones();

                    // if there is an active route
                    if(activeRouteCoordinates.size() > 0){
                        Coordinate nextNearestCoord = null,
                                nearestCoord = null,
                                nextRouteCoord = activeRouteCoordinates.get(1);

//                        String desc = nearbyBeacon.getIdentifier(1) + "-" + nearbyBeacon.getIdentifier(2);
//                        String desc2 = secNearestBeacon.getIdentifier(1) + "-" + secNearestBeacon.getIdentifier(2);

                        String desc = nearbyBeacon.getBluetoothAddress();
                        String desc2 = secNearestBeacon.getBluetoothAddress();

                        // time test
                        long startTime = System.currentTimeMillis();

                        for(Zone zone : currZones) {
                            if(zone.getDescription() != null){
                                if((zone.getDescription()).equals(desc)){
                                    Log.d(LOG_TAG, "MATCH: " + zone.getDescription() + ":" + desc);
                                    nearestBeaconDist = nearbyBeacon.getDistance();
                                    nearestCoord = zone.getZonePoints().get(0);
                                }
                                // next nearest beacon
                                if((zone.getDescription()).equals(desc2)){
                                    Log.d(LOG_TAG, "MATCH2: " + zone.getDescription() + ":" + desc2);

                                    // we know what zone its in
                                    // is that zone in the coordinate list of routes
                                    nextNearestCoord = zone.getZonePoints().get(0);
                                }
                            }
                        }

                        long stopTime = System.currentTimeMillis();
                        long elapsedTime = stopTime - startTime;
                        Log.d(LOG_TAG, "TIMETEST 1: " +Long.toString(elapsedTime));

/*
                        // need to change the hashmap key to be the beacon id instead and change zone description in the MMT tool to be the beacon id
                        // time test2
                        long startTime2 = System.currentTimeMillis();
                        //
                        nearestBeaconDist = nearbyBeacon.getDistance(); // distance to nearest beacon
                        nearestCoord = ((Zone)currZonesMap.get(desc)).getZonePoints().get(0); // nearest beacon
                        nextNearestCoord = ((Zone)currZonesMap.get(desc2)).getZonePoints().get(0); // next nearest beacon

                        long stopTime2 = System.currentTimeMillis();
                        long elapsedTime2 = stopTime2 - startTime2;
                        Log.d(LOG_TAG, "TIMETEST 2: " + Float.toString(elapsedTime2));
*/

                        double  distFromCurrentToNextNearestBeacon = getDistanceInMeters(nextNearestCoord, nearestCoord),
                                distFromNextNearestToNextCoord = getDistanceInMeters(nextNearestCoord, nextRouteCoord);

                        distToNextRouteCoord = getDistanceInMeters(nearestCoord, nextRouteCoord);

                        if(distFromCurrentToNextNearestBeacon < distFromNextNearestToNextCoord){
                            // add to dist between current and next coord
                            totalDistUntilTurn = distToNextRouteCoord + nearestBeaconDist;
                        }
                    }
				}
			}
		});

		try {
			Log.d(BEACON_SIG_TAG, "Ranging: ");
			beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
		} catch (RemoteException e) {  Log.d(BEACON_SIG_TAG, "ranging exception: " + e.toString());  }
	}

	@Override
	public void buildingLoadingCanceled() {

	}



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


			getAllZones();
			String retVal = "You did not say an existing location.";

            // loop through all zones to see if they said one of them
			for(Zone zone : currZones) {
				String zoneName = zone.getName();
				Coordinate zoneCoord = zone.getZonePoints().get(2);
				if(thingsYouSaid.get(0).toLowerCase().equals("route me to " + zoneName.toLowerCase()) ){
					Log.d(VOICE_TAG, "YOU SAID ROUTE ME TO " + zoneName);
					retVal = "Routing to " + zoneName;
					activeDestination = zoneCoord;
					routeToCoord(activeDestination);

				}else if(thingsYouSaid.get(0).toLowerCase().equals("route me to class " + zoneName.toLowerCase()) ){
					Log.d(VOICE_TAG, "YOU SAID ROUTE ME TO CLASS " + zoneName);
					retVal = "Routing to " + zoneName;
					activeDestination = zoneCoord;
					routeToCoord(activeDestination);

				}
			}
			Toast.makeText(this, retVal, Toast.LENGTH_LONG).show();
			speakOut(retVal);
		}
	}

    /**
     * Initializes text to speech engine
     * @param status
     */
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

    /**
     * fires when app is shutdown ( closed completely )
     */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		beaconManager.unbind(this);
	}

    /**
     * fires when app is closed ( still running but not in front )
     */
	@Override
	protected void onStop() {
		super.onStop();

		// shutdown tts!
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