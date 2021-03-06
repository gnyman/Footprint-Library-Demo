package com.walkbase.demoapp;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.walkbase.positioning.Positioning;
import com.walkbase.positioning.Results;
import com.walkbase.positioning.data.WalkbaseLocation;
import com.walkbase.positioning.exceptions.GeneralLibraryException;
import com.walkbase.positioning.listeners.ListRequestListener;
import com.walkbase.positioning.listeners.RecommendationRequestListener;
import com.walkbase.positioning.listeners.VerificationRequestListener;
import com.walkbase.positioning.listeners.dataCommitRequestListener;

public class WalkbaseDemoActivity extends Activity implements VerificationRequestListener, RecommendationRequestListener, ListRequestListener,dataCommitRequestListener {

	private static final String TAG = "WalkbaseDemoActivity.java";
	private Handler handler;
	private Positioning positioning;

    /**
	 * If this is set to true, any verification scans sent are considered as
	 * reference scans and are automatically considered as "trusted" scans.
	 */
	private final boolean CONSIDER_SCANS_AS_REFERENCE_SCANS = false;

	// TODO: Replace this with your own key!
	private final String MY_API_KEY = "507b1679eb729150b06ac609e9f4321d80311800";
	// TODO: Replace this with your own location list!
	private final String LOCATION_LIST_IDENTIFIER = "4e945cd6bff5603f01000001";

	private ArrayList<WalkbaseLocation> recommendations;
	private ArrayList<WalkbaseLocation> footprintLocations;

	private ListView listView;

	private int listType = 0;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (!isOnline()) {
			/**
			 * Check if there is a valid data connection. If not, let the user
			 * know.
			 */
			Toast.makeText(this, "There is no data connection available!\n\nThe application will not function!", Toast.LENGTH_LONG).show();
		}

		// The list view will be displaying the points returned by the server.
		listView = (ListView) findViewById(R.id.locationsList);
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
				Log.d(TAG, "I was clicked");
				AlertDialog.Builder builder = new AlertDialog.Builder(WalkbaseDemoActivity.this);
				builder.setMessage("Check in?").setCancelable(false).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}).setPositiveButton("Once", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						verifyClickedItem(position);
					}
				}).setNeutralButton("Ten times", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        commitData(position);
                    }
                });
				AlertDialog alert = builder.create();
				alert.show();
				// When clicked, verify the user's position using Footprint.

			}
		});

		// We'll need the handler later to run functions on the main thread.
		handler = new Handler();

		// Create a new Positioning object with the current context & your API
		// key.
		positioning = new Positioning(this, MY_API_KEY);
		positioning.setForceWifiOn(false);

		// Get a reference to the scan button in your XML UI.
        Button scanButton = (Button) findViewById(R.id.scanButton);
		scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                /**
                 * Make sure to check that the Positioning class has actually
                 * received some coordinates. If it has not, getCoordinates()
                 * will return NULL.
                 */
                if (positioning.getCoordinates() != null) {
                    String[] ids = {LOCATION_LIST_IDENTIFIER};
                    try {
						positioning.fetchRecommendations(WalkbaseDemoActivity.this, ids);
					} catch (GeneralLibraryException e) {
						Log.d(TAG,"Error fetching recommendations, error was: " + e.getMessage());
						e.printStackTrace();
					}
                } else {
                    // Notify the user that he/she should try again.
                    Toast.makeText(WalkbaseDemoActivity.this, "GPS position not yet available, please try again.", Toast.LENGTH_LONG).show();
                }

            }
        });

		// Get a reference to the list button in your XML UI.
		final ListRequestListener listener = this;
        Button getListButton = (Button) findViewById(R.id.getListButton);
		getListButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // TODO: Remember to replace the value with the ID of your own
                // location database.
                try {
					positioning.fetchLocationList(LOCATION_LIST_IDENTIFIER, listener);
				} catch (GeneralLibraryException e) {
					Log.d(TAG,"Error fetching list, error was: " + e.getMessage());
					e.printStackTrace();
				}
            }
        });
	}

    private void commitData(int position) {
        String[] ids = { LOCATION_LIST_IDENTIFIER };
        if (listType == 1) // list
        {
            if (this.footprintLocations != null && this.footprintLocations.size() > 0) {
                WalkbaseLocation currentWalkbaseLocation = this.footprintLocations.get(position);
                try {
					this.positioning.commitData(this,currentWalkbaseLocation.locationId,currentWalkbaseLocation.locationName,ids,false,5);
				} catch (GeneralLibraryException e) {
					Log.d(TAG,"Error checking in, error was " + e.getMessage());
					e.printStackTrace();
				}
            }
        } else if (listType == 2) {
            if (this.recommendations != null && this.recommendations.size() > 0) {
                WalkbaseLocation currentRecommendation = this.recommendations.get(position);
                try {
					this.positioning.commitData(this, currentRecommendation.locationId, currentRecommendation.locationName, ids, false, 5);
				} catch (GeneralLibraryException e) {
					Log.d(TAG,"Error checking in, error was " + e.getMessage());
					e.printStackTrace();
				}
            }
        }
    }

    /**
	 * Call this with the itemName from the ListView to verify it.
	 * 
	 * @param position  The LocationName of the FootprintLocation / Recommendation
	 */
	private void verifyClickedItem(int position) {
		// Look through the list of recommendations, find the item requested and
		// verify it.
		if (listType == 1) // list
		{
			if (this.footprintLocations != null && this.footprintLocations.size() > 0) {
				WalkbaseLocation currentWalkbaseLocation = this.footprintLocations.get(position);
				verifyItem(currentWalkbaseLocation.locationName, currentWalkbaseLocation.locationId);
			}
		} else if (listType == 2) {
			if (this.recommendations != null && this.recommendations.size() > 0) {
				WalkbaseLocation currentRecommendation = this.recommendations.get(position);
				verifyItem(currentRecommendation.locationName, currentRecommendation.locationId);
			}
		}
	}

	/**
	 * Call this to verify that you are actually at the place described by a
	 * Recommendation or FootprintLocation.
	 * 
	 * @param itemName
	 *            The LocationName of the FootprintLocation / Recommendation
	 * @param itemId
	 *            The LocationId of the FootprintLocation / Recommendation
	 */
	private void verifyItem(String itemName, String itemId) {
		/**
		 * Call the verifyLocation method on the item passed in. The method will
		 * run asynchronously and the response is handled by the
		 * WalkbaseReceiver class.
		 */
		String[] listIds = { Positioning.LIST_ID_FOURQUARE };
		//positioning.verifyLocation(itemId, itemName, CONSIDER_SCANS_AS_REFERENCE_SCANS, Positioning.NORMAL_VERIFICATION_OUTPUT, listIds);
        try {
			positioning.verifyLocation(this,itemId,itemName,listIds,this.CONSIDER_SCANS_AS_REFERENCE_SCANS);
		} catch (GeneralLibraryException e) {
			Log.d(TAG,"Error checking in, error was: " + e.getMessage());
			e.printStackTrace();
		}
	}

    public void updateProgress(final int currentCount,final int totalCount) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(WalkbaseDemoActivity.this, "Progress: " + currentCount + " out of " + totalCount, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void didCommitData(final int successCount) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(WalkbaseDemoActivity.this, "Successfully did a data commit with : " + successCount + " data commits.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void failedToCommitData(final int errorCode,final String message,final int successCount) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(WalkbaseDemoActivity.this, "Could not commit data, error was : " + message + " (managed to do " + successCount + " commits", Toast.LENGTH_LONG).show();
            }
        });
    }

	private boolean isOnline() {

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() == null || cm.getActiveNetworkInfo().isConnectedOrConnecting() == false) {
			return false;
		} else {
			return true;
		}

	}

	public void didGetRecommendation(final ArrayList<WalkbaseLocation> recommendations) {
		Log.d(TAG, "didGetRecommendation got called, yai! Did contain: " + recommendations);
		this.recommendations = recommendations;

		// When redrawing the list, be careful to do it on the main thread.
		handler.post(new Runnable() {
			public void run() {
				try {
					listType = 2;
					listView.setAdapter((new ListItemAdapter(WalkbaseDemoActivity.this, android.R.layout.simple_list_item_1, recommendations)));
				} catch (Exception e) {
					Toast.makeText(WalkbaseDemoActivity.this, "Failed to redraw the list view!", Toast.LENGTH_LONG).show();
				}

			}
		});

	}

	public void failedToGetRecommendation(final int errorCode, final String errorMessage) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(WalkbaseDemoActivity.this, "Failed to get recommendations, errorCode:"+ errorCode +"\n error description was: " + errorMessage, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void didGetVerification(Results results) {
		// Check that results are not NULL and that there's no error with the
		// results.
		if (results != null && results.hasError() == false) {
			final double locationAccuracy = results.getAccuracy();
			final double locationGpsDistance = results.getGPSDistance();
			this.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(WalkbaseDemoActivity.this, "Accuracy: " + locationAccuracy + ", GPS Distance: " + locationGpsDistance, Toast.LENGTH_LONG).show();
				}
			});
		}
	}

	public void failedToGetVerification(final int errorCode, final String errorMessage) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(WalkbaseDemoActivity.this, "Failed to get verification, errorCode:"+ errorCode + "\n error description was: " + errorMessage, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void didGetList(final ArrayList<WalkbaseLocation> recommendations) {

		// When redrawing the list, be careful to do it on the main thread.
		handler.post(new Runnable() {
			public void run() {
				try {
					listType = 1;
					footprintLocations = recommendations;
					listView.setAdapter((new ListItemAdapter(WalkbaseDemoActivity.this, android.R.layout.simple_list_item_1, recommendations)));
				} catch (Exception e) {
					Toast.makeText(WalkbaseDemoActivity.this, "Failed to redraw the list view!", Toast.LENGTH_LONG).show();
				}

			}
		});

	}

	public void failedToGetList(int errorCode, final String errorMessage) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(WalkbaseDemoActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
			}
		});

	}

}