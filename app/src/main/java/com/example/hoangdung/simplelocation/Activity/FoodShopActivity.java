package com.example.hoangdung.simplelocation.Activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.example.hoangdung.simplelocation.Adapter.FoodShopPhotosAdapter;
import com.example.hoangdung.simplelocation.Adapter.FoodShopReviewsAdapter;
import com.example.hoangdung.simplelocation.CircularTextview;
import com.example.hoangdung.simplelocation.EndlessRecyclerOnScrollListener;
import com.example.hoangdung.simplelocation.FirestoreCenter;
import com.example.hoangdung.simplelocation.GoogleDirectionsClient.DirectionsPOJO.DirectionsResponse;
import com.example.hoangdung.simplelocation.GoogleDirectionsClient.GoogleDirectionsQuery;
import com.example.hoangdung.simplelocation.MyApplication;
import com.example.hoangdung.simplelocation.NearestPlacesClient.FoodShopReview;
import com.example.hoangdung.simplelocation.NearestPlacesClient.NearestPlacesPOJO.FoodShop;
import com.example.hoangdung.simplelocation.NearestPlacesClient.NearestPlacesQuery;
import com.example.hoangdung.simplelocation.ProgressWindowAnim;
import com.example.hoangdung.simplelocation.R;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.jpardogo.android.googleprogressbar.library.GoogleProgressBar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.picasso.Picasso;
import com.stepstone.apprating.AppRatingDialog;
import com.stepstone.apprating.listener.RatingDialogListener;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class FoodShopActivity extends AppCompatActivity implements RatingDialogListener {

    @BindView(R.id.food_shop_toolbar)
    public android.support.v7.widget.Toolbar foodShopToolbar;

    @BindView(R.id.food_shop_name)
    public TextView foodShopName;

    @BindView(R.id.food_shop_address)
    public TextView foodShopAdress;

    @BindView(R.id.food_shop_avartar)
    public ImageView foodShopAvartar;


    @BindView(R.id.food_shop_num_reviews)
    public TextView foodShopNumReviews;

    @BindView(R.id.food_shop_num_photos)
    public TextView foodShopNumPhotos;

    @BindView(R.id.food_shop_ratings_text)
    public CircularTextview foodShopRatingText;

    @BindView(R.id.food_shop_photos)
    public RecyclerView foodShopPhotos;

    ProgressWindowAnim<GoogleProgressBar> progressWindowAnim;

    @BindView(R.id.food_shop_reviews_list)
    public RecyclerView foodShopReviewsList;

    @BindView(R.id.foodShopSlidingPanel)
    public SlidingUpPanelLayout foodShopSlidingPanel;

    @BindView(R.id.food_shop_reviews_header)
    public LinearLayout foodShopReviewsHeader;

    /*@BindView(R.id.food_shop_publish_review)
    public ImageButton foodShopPublishReview;*/

    @BindView(R.id.food_shop_reviews_header_text_1)
    public TextView foodShopHeaderText1;

    @BindView(R.id.post_review_button)
    public FloatingActionButton floatingActionButton;

    FoodShop foodShop;

    //Foodshop Photos data
    private long nextMaxPhotos = 20;
    private long numOfTotalPhotos;
    private DocumentSnapshot lastPhotoSnapshot;

    //Foodshop Reviews data
    private long nextMaxReviews = 20;
    private long numOfTotalsReviews;
    private DocumentSnapshot lastReviewSnapshot;
    private ListenerRegistration reviewsChangeListener;

    //FoodShop data
    private ListenerRegistration shopChangeListener;

    //Used for EventListener of Reviews, Photos, Shop
    //Because EventListener will always be called when it is invoked the first time,
    //We may want keep track of this behavior
    boolean shopLoadingFirstTime = true;
    boolean photosLoadingFirstTime = true;
    boolean reviewsLoadingFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_shop);
        ButterKnife.bind(this);
        progressWindowAnim = new ProgressWindowAnim(this, R.layout.progress_window_layout_2);
        foodShop = getIntent().getParcelableExtra("shop");
        setSupportActionBar(foodShopToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupFoodShopInfo();
    }

    //Setup FoodShop Information
    void setupFoodShopInfo() {
        setupShopContent();
        setupPanelLayout();

    }
    private void setupShopContent() {
        Log.d("MapsActivity", "shopid: " + String.valueOf(foodShop.shopID));
        //Load Image
        Picasso.with(this)
                .load(foodShop.avartar)
                .fit()
                .into(foodShopAvartar);
        //Display Food Shop Name
        foodShopName.setText(foodShop.name);
        //Display Food Shop Adress
        foodShopAdress.setText(foodShop.address);


        //Show comment dialog when floating action button is clicked
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        //Add listener when there are changes to this food shop
        //Also listener will be called when we invoke it the first time
        //Use this to get latest update of food shop
        shopChangeListener = FirestoreCenter.Companion.getInstance().listenToShop(
                foodShop.shopID,
                new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                        //Save and Display Rating Text
                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                        decimalFormat.setRoundingMode(RoundingMode.CEILING);
                        double newRatings = Double.parseDouble(documentSnapshot.get("averageRatings").toString());
                        foodShop.averageRatings = newRatings;
                        foodShopRatingText.setText(decimalFormat.format(newRatings));
                        foodShopRatingText.setSolidColor(getResources().getColor(R.color.green));

                        //Save number of total photos, number of total reviews
                        foodShop.numOfPhotos = (long) documentSnapshot.get("numOfPhotos");
                        numOfTotalPhotos = foodShop.numOfPhotos;
                        foodShop.numOfRatings = (long) documentSnapshot.get("numOfRatings");
                        numOfTotalsReviews = foodShop.numOfRatings;

                        //Display Food Shop Num Reviews
                        foodShopNumReviews.setText(String.valueOf(foodShop.numOfRatings));
                        //Display Food Shop Num Photos
                        foodShopNumPhotos.setText(String.valueOf(foodShop.numOfPhotos));

                        //Setup Photo and Review features
                        if (shopLoadingFirstTime) {
                            setupPhotosContent();
                            setupReviewsContent();
                            shopLoadingFirstTime = false;
                        }

                    }
                }
        );


    }

    private void setupPhotosContent() {
        //Init ListView
        foodShopPhotos.setHasFixedSize(true);
        final FoodShopPhotosAdapter adapter = new FoodShopPhotosAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        foodShopPhotos.setLayoutManager(layoutManager);
        foodShopPhotos.setAdapter(adapter);
        //Query the first 20 photos
        //Get Photos Collection Reference of Food Shop
        FirestoreCenter.Companion.getInstance().getPhotos(
                foodShop.shopID,
                nextMaxPhotos,
                null,
                new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<String> photoUrls = new ArrayList<>();
                            for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                photoUrls.add((String) documentSnapshot.get("photoUrl"));
                            }
                            adapter.setPhotosUrl(photoUrls);
                            adapter.notifyDataSetChanged();
                            lastPhotoSnapshot = task.getResult().getDocuments().get(task.getResult().getDocuments().size() - 1);
                        }

                    }


                }
        );
        //Implements EndlessScroll For RecyclerView
        foodShopPhotos.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int current_page) {
                //Find the number of remaining phots
                long restNumOfPhotos = numOfTotalPhotos - previousTotal;
                Log.d("MapsActivity", "numOfTotalPhotos: " + String.valueOf(numOfTotalPhotos));
                Log.d("MapsActivity", "previousTotal: " + String.valueOf(previousTotal));
                Log.d("MapsActivity", "rest: " + String.valueOf(restNumOfPhotos));
                //If there are no photos to query, return
                if (restNumOfPhotos == 0)
                    return;
                //If it is larger than number of photos we going to load next, use nextMaxPhotos to query
                long numOfNextPhotos = 0;
                if (restNumOfPhotos >= nextMaxPhotos) {
                    numOfNextPhotos = nextMaxPhotos;
                } else //use number of remaining photos to query
                {
                    numOfNextPhotos = restNumOfPhotos;
                }
                final long finalNumOfNextPhotos = numOfNextPhotos;
                FirestoreCenter.Companion.getInstance().getPhotos(
                        foodShop.shopID,
                        numOfNextPhotos,
                        lastPhotoSnapshot,
                        new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    ArrayList<String> photoUrls = adapter.getPhotosUrl();
                                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                        photoUrls.add((String) documentSnapshot.get("photoUrl"));
                                    }
                                    lastPhotoSnapshot = task.getResult().getDocuments().get(task.getResult().getDocuments().size() - 1);
                                    adapter.notifyItemRangeInserted(previousTotal, task.getResult().getDocuments().size() - 1);
                                }

                            }
                        }
                );
            }
        });
    }

    private void setupReviewsContent() {
        //Setup reviews recyclerview
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        foodShopReviewsList.setLayoutManager(linearLayoutManager);
        final FoodShopReviewsAdapter adapter = new FoodShopReviewsAdapter();
        foodShopReviewsList.setAdapter(adapter);
        foodShopReviewsList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        //Set EndlessScroll for endless data
        foodShopReviewsList.addOnScrollListener(new EndlessRecyclerOnScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int current_page) {
                long restNumOfReviews = numOfTotalsReviews - previousTotal;
                //If there are no reviews , return
                if (restNumOfReviews == 0)
                    return;
                //If it is larger than number of photos we going to load next, use nextMaxPhotos to query
                long nextNumOfReviews = 0;
                if (restNumOfReviews >= nextMaxReviews) {
                    nextNumOfReviews = nextMaxReviews;
                } else //use number of remaining photos to query
                {
                    nextNumOfReviews = restNumOfReviews;
                }
                FirestoreCenter.Companion.getInstance().getReviews(
                        foodShop.shopID,
                        nextNumOfReviews,
                        lastReviewSnapshot,
                        new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    if (task.getResult().getDocuments().size() == 0)
                                        return;
                                    ArrayList<FoodShopReview> reviewsArrayList = new ArrayList<>();
                                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                        reviewsArrayList.add(documentSnapshot.toObject(FoodShopReview.class));
                                    }
                                    lastReviewSnapshot = task.getResult().getDocuments().get(task.getResult().getDocuments().size() - 1);
                                    adapter.setFoodShopReviews(reviewsArrayList);
                                    adapter.notifyItemRangeInserted(previousTotal, task.getResult().getDocuments().size() - 1);
                                }

                            }
                        }
                );
            }
        });
        //Query first 20 reviews
        FirestoreCenter.Companion.getInstance().getReviews(
                foodShop.shopID,
                nextMaxReviews,
                null,
                new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().getDocuments().size() == 0)
                                return;
                            ArrayList<FoodShopReview> reviewsArrayList = new ArrayList<>();
                            for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                reviewsArrayList.add(documentSnapshot.toObject(FoodShopReview.class));
                            }
                            lastReviewSnapshot = task.getResult().getDocuments().get(task.getResult().getDocuments().size() - 1);
                            adapter.setFoodShopReviews(reviewsArrayList);
                            adapter.notifyDataSetChanged();

                        }
                    }
                }
        );

        //Add Listener for reviews changes
        //The listener will be always called the first time when this function is invoked eventhough the data are not changed
        //We skip that first call and keep track of later calls
        reviewsChangeListener = FirestoreCenter.Companion.getInstance().listenToReviewChanges(
                foodShop.shopID,
                new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot querySnapshot, FirebaseFirestoreException e) {
                        if (reviewsLoadingFirstTime) {
                            reviewsLoadingFirstTime = false;
                            Log.d("MapsActivity","FirstTime loading reviews");
                            return;
                        }
                        for (DocumentChange documentChange : querySnapshot.getDocumentChanges()) {

                            //If this review is newly added
                            //Add it to adapter and notify changes for recycler view to update UI
                            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                Log.d("MapsActivity", "There is changes in reviews");
                                FoodShopReview foodShopReview = new FoodShopReview();
                                foodShopReview.fromMap(documentChange.getDocument().getData());
                                adapter.getFoodShopReviews().add(0, foodShopReview);
                                adapter.notifyItemInserted(0);
                            }
                        }
                    }
                }
        );

    }

    private void setupPanelLayout() {
        //Set up Sliding Panel Layout
        foodShopReviewsHeader.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                foodShopSlidingPanel.setPanelHeight(foodShopReviewsHeader.getHeight());
                                foodShopReviewsHeader.getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                            }
                        }
                );


    }

    private void showDialog() {
        new AppRatingDialog.Builder()
                .setPositiveButtonText("Submit")
                .setNegativeButtonText("Cancel")
                .setNeutralButtonText("Later")
                .setNoteDescriptions(Arrays.asList("Very Bad", "Not good", "Quite ok", "Very Good", "Excellent !!!"))
                .setDefaultRating(2)
                .setTitle("Rate this food shop")
                .setDescription("Hãy chọn mức yêu thích và điền feedback")
                .setStarColor(R.color.reviewsDialogStarColor)
                .setNoteDescriptionTextColor(R.color.noteDescriptionTextColor)
                .setTitleTextColor(R.color.titleTextColor)
                .setDescriptionTextColor(R.color.contentTextColor)
                .setHint("Please write your comment here ...")
                .setHintTextColor(R.color.hintTextColor)
                .setCommentTextColor(R.color.commentTextColor)
                .setCommentBackgroundColor(R.color.colorPrimaryDark)
                .setWindowAnimation(R.style.MyDialogFadeAnimation)
                .create(FoodShopActivity.this)
                .show();
    }

    @Override
    public void onPositiveButtonClicked(int i, String s) {
        if (s.isEmpty()) {
            Toast.makeText(this, "You should also fill feedback", Toast.LENGTH_LONG);
            return;
        }
        progressWindowAnim.showProgress();
        FoodShopReview review = new FoodShopReview();
        review.comment = s;
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        review.ratings = Double.parseDouble(decimalFormat.format(i * 10 / 5));
        review.userID = FirestoreCenter.Companion.getInstance().getDbAuth().getUid();
        FirestoreCenter.Companion.getInstance().publishReview(
                foodShop.shopID,
                review,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            progressWindowAnim.hideProgress();
                            Toast.makeText(FoodShopActivity.this, "Your comment is added", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    public void onNegativeButtonClicked() {

    }

    @Override
    public void onNeutralButtonClicked() {

    }

    @Override
    protected void onStop() {
        super.onStop();
        shopChangeListener.remove();
        reviewsChangeListener.remove();
    }

    @Override
    public void onBackPressed() {
        if (foodShopSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            foodShopSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            setResult(RESULT_CANCELED);
            finish();

        }

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.find_direction) {
            progressWindowAnim.showProgress();
            final Intent intent = new Intent(this,FoodShopDirectionActivity.class);
            Task<Location> lastLocationTask = LocationServices.getFusedLocationProviderClient(this).getLastLocation();
            lastLocationTask.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.isSuccessful()){
                        Location location = task.getResult();
                        final double lat = task.getResult().getLatitude();
                        final double lng = task.getResult().getLongitude();
                        GoogleDirectionsQuery query = new GoogleDirectionsQuery.Builder()
                                .withOrigin(new LatLng(lat,lng))
                                .withDestination(new LatLng(foodShop.lat,foodShop.lng))
                                .buid();
                        query.query(new GoogleDirectionsQuery.OnDirectionsResultListener() {
                            @Override
                            public void onDirectionsResult(DirectionsResponse directionsResponse, int resultCode) {
                                if(resultCode == GoogleDirectionsQuery.RESPONSE_SUCCESS){
                                    intent.putExtra("direction",directionsResponse);
                                    intent.putExtra("source",new LatLng(lat,lng));
                                    intent.putExtra("dest",new LatLng(foodShop.lat,foodShop.lng));
                                    startActivity(intent);
                                }
                                else
                                {
                                    Toast.makeText(FoodShopActivity.this,"Something wrong happened, try again!",Toast.LENGTH_LONG).show();
                                }
                                progressWindowAnim.hideProgress();
                            }
                        });

                    }
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.food_shop_menu, menu);
        return true;
    }
}
