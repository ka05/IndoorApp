package com.mycompany.myfirstindoorsapp;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
//import android.view.View.OnClickListener;


//import com.example.android.actionbarcompat.ActionBarActivity;

/**
 * Sample Android project, powered by indoo.rs :)
 *
 * @author indoo.rs | Philipp Koenig
 *
 */

// May want to redisgn using Material design
// http://alexzh.com/tutorials/material-style-for-dialogs-in-android-application/


public class MainActivity extends FragmentActivity implements IndoorsLocationListener {
	protected static final int REQUEST_OK = 1;
	protected static final String LOG = "Voice";
	private static final String LOG_TAG = "DEBUG: ";
	//public class MainActivity extends ActionBarActivity implements IndoorsLocationListener {

	private IndoorsSurfaceFragment indoorsFragment;
	private SurfaceState surfaceState;
	private IndoorsSurface indoorsSurface;

	private Menu menu;
	final Context context = this;

	private volatile EditText startText;
	private EditText endText;
	private Button cancel, display;
	public List<Zone> currZones;

	static int count = 0;
	ArrayList<String> ar = new ArrayList<String>();

	Coordinate start1;
	Coordinate end1;
	//private ZoneSelectionFragment zoneSelectionFragment
	//private IndoorsSurfaceFragment indoorsSurfaceFragment;

	Building building;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.layout);
		super.onCreate(savedInstanceState);
		IndoorsFactory.Builder indoorsBuilder = new IndoorsFactory.Builder();
		IndoorsSurfaceFactory.Builder surfaceBuilder = new IndoorsSurfaceFactory.Builder();

		indoorsBuilder.setContext(this);

//		indoorsBuilder.setApiKey("2dafb500-5aa0-4af8-b40f-5aec82ed50e4"); // old
		indoorsBuilder.setApiKey("fc6d16ff-e3b1-4b2a-b43e-d09c5f152063");

		// TODO: replace 12345 with the id of the building you uploaded to
		// our cloud using the MMT
//		indoorsBuilder.setBuildingId((long) 466393307); // Old
		indoorsBuilder.setBuildingId((long) 559582343); // 559582343 - curr
		indoorsBuilder.setEvaluationMode(false);


		// callback for indoo.rs-events
		indoorsBuilder.setUserInteractionListener(this);

		// TODO: load bitmaps
//		Bitmap navigationArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow);;
////		Bitmap navigationPoint = null;
//
//		SurfacePainterConfiguration configuration = DefaultSurfacePainterConfiguration.getConfiguration();
//		configuration.setNavigationArrow(navigationArrow);
////		configuration.setNavigationPoint(navigationPoint);
//
//		surfaceBuilder.setSurfacePainterConfiguration(configuration);

		surfaceBuilder.setIndoorsBuilder(indoorsBuilder);
		indoorsFragment = surfaceBuilder.build();
		indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_CURRENT_ZONE);
		indoorsFragment.getSurfaceState().orientedNaviArrow = true; // set navgigation arrow on


		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(android.R.id.content, indoorsFragment, "indoors");
		transaction.commit();
		/*
		if (getResources().getBoolean(R.bool.default_show_all_zones)) {
			indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_ALL_ZONES);
		}

		if (getResources().getBoolean(R.bool.default_zone_fencing_enabled)) {
			indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_CURRENT_ZONE);
		}
		*/


		// set up speech recognizer
		Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
		try {
			startActivityForResult(i, REQUEST_OK);
		} catch (Exception e) {
			Toast.makeText(this, "Error initializing speech to text engine.", Toast.LENGTH_LONG).show();
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==REQUEST_OK  && resultCode==RESULT_OK) {
			ArrayList<String> thingsYouSaid = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//			((TextView)findViewById(R.id.text1)).setText(thingsYouSaid.get(0));

			String whole =  "";

			for(String thing : thingsYouSaid){
				whole += " " + thing;
			}
			whole = whole.substring(1); //remove first space
			Log.d(LOG, whole);

			/*
			switch(thingsYouSaid.get(0).toLowerCase()){
				case "this is sweet":
//					((TextView)findViewById(R.id.text1)).setText("Darn Right it is!");
					break;
				case "android is awesome":
//					((TextView)findViewById(R.id.text1)).setText("Yes I am");
					break;
				case "exit":
				case "close":
//					((TextView)findViewById(R.id.text1)).setText("Exiting");

					// close app
					finish();
					System.exit(0);
					break;
			}
			*/
		}
	}


	@Override
	protected void onStop() {
		super.onStop();

//		indoors.removeLocationListener(this);

//		IndoorsFactory.releaseInstance(this);
	}

	// need to use this to update view ( need accurate res )
	public void positionUpdated(Coordinate userPosition, int accuracy) {
		//System.out.println("positionUpdated");

		Coordinate userPosCoord = indoorsFragment.getCurrentUserPosition();
		Toast.makeText(this, "getCurrentUserPosition: " + userPosCoord.toString(), Toast.LENGTH_SHORT).show();

		GeoCoordinate geoCoordinate = indoorsFragment.getCurrentUserGpsPosition();

		if (geoCoordinate != null) {
			Toast.makeText(
					this,
					"User is located at " + geoCoordinate.getLatitude() + ","
							+ geoCoordinate.getLongitude(), Toast.LENGTH_SHORT).show();
		}

		// Presently the below function is calculating the static route
		//calculateRoute();


	}


	@Override
	public void loadingBuilding(LoadingBuildingStatus loadingBuildingStatus) {
		int progress = loadingBuildingStatus.getProgress();
	}

	public void buildingLoaded(Building building) {
		// indoo.rs SDK successfully loaded the building you requested and
		// calculates a position now
		System.out.println("buildingLoaded");
		Toast.makeText(
				this,
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
				System.out.println("floor:" + name);
				//sendResult("changedFloor", floorLevel + "|" + name, "message", PluginResult.Status.OK);
			}
		} catch (Exception e) {
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
		//System.out.println("testing");
//		Toast.makeText(this,"User Direction " + orientation, Toast.LENGTH_SHORT).show();
//		System.out.print("orientation: " +orientation);
	}

	public void enteredZones(List<Zone> zones) {

		//System.out.println("size:" + zones.size());
		for (Zone zone : zones) {
			Toast.makeText(this, "You are at the Zone " + zone.getName(), Toast.LENGTH_SHORT).show();

		}

		// see if we can get proximity here

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
				//Toast.makeText(MainActivity.this, "Route is Selected", Toast.LENGTH_SHORT).show();
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
						//indoorsSurface.updatePainter();
//						calculateRoute(startText.getText().toString(),endText.getText().toString());
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
								Log.d(LOG_TAG, "Coords: " + arg0.get(arg0.size()- 1).x + ", " + arg0.get(arg0.size()- 1).y);
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
						//System.out.println("in cancel");
						dialog.cancel();

					}
				});
				dialog.show();
				return true;

			case R.id.menu_search_invoke:
				// show all zones here and add on click listener to each to calc route
				final Dialog searchDialog = new Dialog(MainActivity.this);
				searchDialog.setContentView(R.layout.search_routes);
				searchDialog.setTitle("Select a destination");

				TableLayout tblLayout = (TableLayout)searchDialog.findViewById(R.id.tblSearchRoutes);
				tblLayout.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

				cancel = (Button) searchDialog.findViewById(R.id.search_cancel);

				indoorsFragment.setViewMode(ViewMode.HIGHLIGHT_ALL_ZONES);
				currZones = indoorsFragment.getZones(); // gets all zones

				for(Zone zone : currZones){
					Log.d(LOG_TAG, "Zone Name: " + zone.getName());
					TableRow row = new TableRow(this);
					row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

					TextView t = new TextView(this);
					t.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

					int tenDp = getPixelsByDp(10);
					t.setPadding(tenDp, tenDp, tenDp, tenDp);

					// get first coordinate in arraylist for given zone
					Coordinate zoneCoord = ((ArrayList < Coordinate >)zone.getZonePoints()).get(2);
					String zoneCoordString = zoneCoord.x + "," + zoneCoord.y + "," + zoneCoord.z;
					t.setContentDescription( zoneCoordString );
//					t.setContentDescription( zone.getName() );

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
							searchDialog.dismiss();
							Log.d(LOG_TAG, "Coords" + coords[0] + ", " + coords[1] + ", " + coords[2]);
							// get current position
//							calculateRoute("lab", zoneName);
//							indoorsFragment.routeTo(zoneCoord, false);

							// might not be working because im not in the building
							indoorsFragment.getIndoors().getRouteAToB(indoorsFragment.getCurrentUserPosition(), zoneCoord, new RoutingCallback() {

								@Override
								public void onError(IndoorsException arg0) {
									// TODO Auto-generated method stub
									Log.d(LOG_TAG, "Routing error" + arg0.toString());
								}

								@Override
								public void setRoute(ArrayList<Coordinate> arg0) {
									Log.d(LOG_TAG, "Coords: " + arg0.get(arg0.size()- 1).x + ", " + arg0.get(arg0.size()- 1).y);
									Toast.makeText(context, "setRoute" +  arg0.get(0).x + ", " + arg0.get(0).y, Toast.LENGTH_LONG).show();
									indoorsFragment.getSurfaceState().setRoutingPath(arg0, false);

									indoorsFragment.updateSurface();
								}
							});
						}
					});
					// set onclick listener
					row.addView(t);
					tblLayout.addView(row);
				}

				cancel.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						//System.out.println("in cancel");
						searchDialog.cancel();
					}
				});

				searchDialog.show();

				return true;

			case R.id.menu_show_all_zones:
				item.setChecked(!item.isChecked());
				refreshViewMode();
				return true;
		}

		return super.onOptionsItemSelected(item);
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

	public void calculateRoute(String start, String end) {

		int startX = 0;
		int startY = 0;
		int endX = 0;
		int endY = 0;
		indoorsFragment.updateSurface();
		System.out.println("start name:" + start);
		System.out.println("end name:" + end);

		for (Zone zone : indoorsFragment.getZones()) {
			System.out.println("Zone Name:" + zone.getName());
			/*ArrayList<Zone> zones = new ArrayList<Zone>();
			for (Zone zone1 : zones) {
				//	Toast.makeText(this, "You are at the Zone " + zone.getName(), Toast.LENGTH_SHORT).show();
				System.out.println("vvvv:"+zone1.getName());
			}*/
			ar.add(zone.getName());
			System.out.println("vvvv:" + ar);

				if (zone.getName().equals(start)) {
				for (Coordinate c : zone.getZonePoints()) {
					System.out.println("one");
					System.out.println("source-x:"+c.x);
					System.out.println("source-y:"+c.y);
					startX += c.x;
					startY += c.y;
				}
					startX /= zone.getZonePoints().size();
					startY /= zone.getZonePoints().size();
					//System.out.println("startX:"+startX);
					//System.out.println("startY:"+startY);
				}
				else if (zone.getName().equals(end)) {
				for (Coordinate c : zone.getZonePoints()) {
					System.out.println("two");
					System.out.println("end-x:"+c.x);
					System.out.println("end-y:"+c.y);
					endX += c.x;
					endY += c.y;
				}
					endX /= zone.getZonePoints().size();
					endY /= zone.getZonePoints().size();
					//System.out.println("endX:" + endX);
					//System.out.println("endY:"+endY);
				}/*
				else {
					Toast.makeText(this, "No Zones found!", Toast.LENGTH_SHORT)
							.show();
					startX = 0;
					startY = 0;
					endX = 0;
					endY = 0;
					//System.exit(0);
				}*/

			//System.out.println("X:"+(startX-endX));
			//System.out.println("Y:"+(startY-endY));
			//Coordinate start1 = new Coordinate(startX, startY, 3);
			//Coordinate end1 = new Coordinate(endX, endY, 3);
			System.out.println("startX:"+startX);
			System.out.println("startY:"+startY);

			System.out.println("endX:" + endX);
			System.out.println("endY:" + endY);
			if((startX == 0) || (startY == 0) || (endX == 0) || (endY == 0) ){
				 start1 = new Coordinate(0, 0, 3);
				 end1 = new Coordinate(0, 0, 3);
			}
			else{
				 start1 = new Coordinate(startX, startY, 3);
				 end1 = new Coordinate(endX, endY, 3);
			}
			indoorsFragment.getIndoors().getRouteAToB(start1, end1, new RoutingCallback() {
				@Override
				public void onError(IndoorsException arg0) {
					// TODO Auto-generated method stub
				}

				@Override
				public void setRoute(ArrayList<Coordinate> arg0) {
					indoorsFragment.getSurfaceState().setRoutingPath(arg0, true);
					indoorsFragment.updateSurface();
				}
			});

		}

	}
}

//grep -rnw * -e "menu_search_invoke"
//RoutingViewFragment.java