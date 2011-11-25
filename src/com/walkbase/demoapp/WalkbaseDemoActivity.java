package com.walkbase.demoapp;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.walkbase.positioning.Positioning;
import com.walkbase.positioning.Results;
import com.walkbase.positioning.data.FootprintLocation;
import com.walkbase.positioning.data.Recommendation;

public class WalkbaseDemoActivity extends Activity {

	private Handler handler;
	private Positioning positioning;
	private Button scanButton;
	private Button getListButton;

	/**
	 *  If this is set to true, any verification scans sent are considered as
	 *  reference scans and are automatically considered as "trusted" scans.
	 */
	private final boolean CONSIDER_SCANS_AS_REFERENCE_SCANS = false;

	// TODO: Replace this with your own key!
	private final String MY_API_KEY = "06a04f9fee214a0062f44c8848fe452650acb94e";
	// TODO: Replace this with your own location list!
	private final String LOCATION_LIST_IDENTIFIER = "4e945cd6bff5603f01000001";

	private ArrayList<Recommendation> recommendations;
	private ArrayList<FootprintLocation> footprintLocations;

	private WalkbaseReceiver walkbaseReceiver;
	private ListView listView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if(isOnline() == false) {
			/**
			 * Check if there is a valid data connection. If not, let the user know.
			 */
			Toast.makeText(this, "There is no data connection available!\n\nThe application will not function!", Toast.LENGTH_LONG).show();
		}


		// The list view will be displaying the points returned by the server.
		listView = (ListView) findViewById(R.id.locationsList);
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, verify the user's position using Footprint.
				verifyClickedItem(((TextView) view).getText().toString()); 
			}
		});



		// We'll need the handler later to run functions on the main thread.
		handler = new Handler();

		//Create a new Positioning object with the current context & your API key.
		positioning = new Positioning(this, MY_API_KEY);

		// Create & register the intent receiver
		walkbaseReceiver = new WalkbaseReceiver();
		this.registerReceiver(walkbaseReceiver, new IntentFilter(positioning.getPositioningIntentString()));

		// Get a reference to the scan button in your XML UI.
		scanButton = (Button) findViewById(R.id.scanButton);
		scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				/**
				 *  Make sure to check that the Positioning class has actually received some coordinates.
				 *  If it has not, getCoordinates() will return NULL.
				 */
				if(positioning.getCoordinates() != null) {
					double[] coordinates = positioning.getCoordinates();
					double latitude = coordinates[0];
					double longitude = coordinates[1];
					double accuracy = positioning.getGPSAccuracy();
					
					positioning.fetchRecommendations(latitude, longitude, accuracy);
				}
				
				else {
					// Notify the user that he/she should try again.
					Toast.makeText(WalkbaseDemoActivity.this, "GPS position not yet available, please try again.", Toast.LENGTH_LONG).show();
				}
				
			}
		});

		// Get a reference to the list button in your XML UI.
		getListButton = (Button) findViewById(R.id.getListButton);
		getListButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//TODO: Remember to replace the value with the ID of your own location database.
				positioning.fetchLocationList(LOCATION_LIST_IDENTIFIER);
			}
		});
	}

	/**
	 * Call this with the itemName from the ListView to verify it.
	 * @param String The LocationName of the FootprintLocation / Recommendation
	 */
	private void verifyClickedItem(String itemName)  {
		// Look through the list of recommendations, find the item requested and verify it.
		if(this.recommendations != null && this.recommendations.size() > 0) {
			for(int i = 0; i < this.recommendations.size(); i++) {
				Recommendation currentRecommendation = this.recommendations.get(i);

				if(currentRecommendation.getLocationName().equals(itemName)) {
					verifyItem(currentRecommendation.getLocationName(), currentRecommendation.getLocationId());
					return; // We found the match, don't continue looking.
				}
			}
		}


		// Look through the list of your own locations, find the item requested and verify it.
		if(this.footprintLocations != null && this.footprintLocations.size() > 0) {
			for(int i = 0; i < this.footprintLocations.size(); i++) {
				FootprintLocation currentFootprintLocation = this.footprintLocations.get(i);

				if(currentFootprintLocation.getLocationName().equals(itemName)) {
					verifyItem(currentFootprintLocation.getLocationName(), currentFootprintLocation.getLocationId());
					return; // We found the match, don't continue looking.
				}
			}
		}
	}

	/**
	 * Call this to verify that you are actually at the place described by a Recommendation or FootprintLocation.
	 * @param String The LocationName of the FootprintLocation / Recommendation
	 * @param String The LocationId of the FootprintLocation / Recommendation
	 */
	private void verifyItem(String itemName, String itemId) {
		/**
		 * Call the verifyLocation method on the item passed in. The method will run asynchronously 
		 * and the response is handled by the WalkbaseReceiver class.
		 */
		positioning.verifyLocation(itemId, itemName, CONSIDER_SCANS_AS_REFERENCE_SCANS, Positioning.NORMAL_VERIFICATION_OUTPUT);
	}



	public class WalkbaseReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {

			/**
			 * First, we check if there was an
			 * error completing the request.
			 * 
			 * If there was, you might want to display
			 * a message to the user or try your request again.
			 */
			if(intent.hasExtra(Positioning.POSITIONING_HAS_ERROR)){
				String errorMessage = intent.getStringExtra(Positioning.POSITIONING_ERROR_MESSAGE);
				Log.e("Error", errorMessage);
				Toast.makeText(WalkbaseDemoActivity.this, "Failed with error: " + errorMessage, Toast.LENGTH_LONG).show();
				return;
			}


			else { 
				// No errors -> Get intent type.
				int intentType = intent.getIntExtra(Positioning.POSITIONING_INTENT_TYPE, 0);

				if(intentType == Positioning.ASSISTED_RECOMMENDATION || intentType == Positioning.NORMAL_RECOMMENDATION) {
					// Check that the list has actual content.
					if(positioning.getRecommendations() != null && positioning.getRecommendations().size() > 0) {
						recommendations = positioning.getRecommendations();

						// Create a version of the list that includes only the names -> can be directly used for the ArrayAdapter.
						ArrayList<String> readableList= new ArrayList<String>();
						for(int i = 0; i < recommendations.size(); i++) {
							Recommendation r = recommendations.get(i);
							readableList.add(r.getLocationName());
							Log.d("Parsed Point", r.getLocationName());
						}
						// We need a final version of the list to be able to access it in a worker thread.
						final ArrayList<String> finalReadableList = readableList;

						// When redrawing the list, be careful to do it on the main thread.
						handler.post(new Runnable() {
							public void run() {
								try{ 
									listView.setAdapter((new ArrayAdapter<String>(WalkbaseDemoActivity.this, android.R.layout.simple_list_item_1, finalReadableList)));
								}catch(Exception e){
									Toast.makeText(WalkbaseDemoActivity.this, "Failed to redraw the list view!", Toast.LENGTH_LONG).show();
								}

							}
						});

					}
				} // End if ASSISTED_RECOMMENDATION || NORMAL_RECOMMENDATION

				else if(intentType == Positioning.LOCATION_LIST_RESPONSE) {
					// Check that the list has actual content.
					if(positioning.getLocationListResult() != null && positioning.getLocationListResult().size() > 0) {
						footprintLocations = positioning.getLocationListResult();

						// Create a version of the list that includes only the names -> can be directly used for the ArrayAdapter.
						ArrayList<String> readableList= new ArrayList<String>();
						for(int i = 0; i < footprintLocations.size(); i++) {
							FootprintLocation fp = footprintLocations.get(i);
							readableList.add(fp.getLocationName());
							Log.d("Parsed Location", fp.getLocationName());
						}

						// We need a final version of the list to be able to access it in a worker thread.
						final ArrayList<String> finalReadableList = readableList;	

						// When redrawing the list, be careful to do it on the main thread.
						handler.post(new Runnable() {
							public void run() {
								try{ 
									listView.setAdapter((new ArrayAdapter<String>(WalkbaseDemoActivity.this, android.R.layout.simple_list_item_1, finalReadableList)));
								}catch(Exception e){
									Toast.makeText(WalkbaseDemoActivity.this, "Failed to redraw the list view!", Toast.LENGTH_LONG).show();
								}

							}
						});
					}
				} // End if LOCATION_LIST_RESPONSE


				else if(intentType == Positioning.ASSISTED_VERIFICATION || intentType == Positioning.NORMAL_VERIFICATION) {
					Results results = positioning.getVerificationResults();

					// Check that results are not NULL and that there's no error with the results.
					if(results != null && results.hasError() == false) {
						final double locationAccuracy = results.getAccuracy();
						final double locationGpsDistance = results.getGPSDistance();

						// Now we can do something fun with the results, for example display them to the user.
						handler.post(new Runnable() {
							public void run() {
								try{ 
									Toast.makeText(WalkbaseDemoActivity.this, "Accuracy: " + locationAccuracy + ", GPS Distance: " + locationGpsDistance, Toast.LENGTH_LONG).show();
								}catch(Exception e){
								}
							}
						});
					}
				} // End if ASSISTED_VERIFICATION or NORMAL_VERIFICATION


			}
		}
	}

	
	private boolean isOnline() {
		
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if(cm.getActiveNetworkInfo() == null || cm.getActiveNetworkInfo().isConnectedOrConnecting() == false){
			return false; 
		}	
		else{
			return true;
		}

	}

}