package firebase.cabsy.Account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import firebase.cabsy.R;
import firebase.cabsy.Utils.FirebaseMethods;
import firebase.cabsy.Utils.UniversalImageLoader;
import firebase.cabsy.models.User;

import static android.app.Activity.RESULT_OK;

public class CarUpdateFragment extends Fragment {

    private static final String TAG = "CarUpdateFragment";
    private static final int GALLARY_PICK = 001;
    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabse;
    private DatabaseReference mRef;
    private FirebaseMethods mFirebaseMethods;
    private String userID;

    //Fragment view
    private View view;
    private CircleImageView mCarPhoto;
    private EditText mCar, mRegistration, mLicence, mSeats;
    private Button mSnippetCarBtn;
    private RadioGroup mCarOwnerRadioGroup;
    private RadioButton ownerYesButton, ownerNoButton;
    private RelativeLayout mCarDetailsLayout;
    private ProgressDialog loadingbar;

    //vars
    private User mUserSettings;
    private Boolean carOwner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_update_car, container, false);
        mCarPhoto = (CircleImageView) view.findViewById(R.id.uploadCarPicture);
        mCar = (EditText) view.findViewById(R.id.carEditText);
        mRegistration = (EditText) view.findViewById(R.id.registrationEditText);
        mLicence = (EditText) view.findViewById(R.id.licenceEditText);
        mSeats = (EditText) view.findViewById(R.id.seatsEditText);
        mCarOwnerRadioGroup = (RadioGroup) view.findViewById(R.id.carToggle);
        ownerYesButton = (RadioButton) view.findViewById(R.id.yesCarButton);
        ownerNoButton = (RadioButton) view.findViewById(R.id.noCarButton);
        mCarDetailsLayout = (RelativeLayout) view.findViewById(R.id.carDetailsLayout);
        loadingbar = new ProgressDialog(getActivity());

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabse = FirebaseDatabase.getInstance();
        mRef = mFirebaseDatabse.getReference();
        mFirebaseMethods = new FirebaseMethods(getActivity());


        mSnippetCarBtn = (Button) view.findViewById(R.id.snippetCarDetailsBtn);
        mSnippetCarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileSettings();
            }
        });
        setupFirebaseAuth();

        getUserInformation();


        //Setup back arrow for navigating back to 'ProfileActivity'
        ImageView backArrow = (ImageView) view.findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating back to ProfileActivity");
                Intent intent = new Intent(view.getContext(), AccountActivity.class);
                startActivity(intent);
            }
        });

        mCarOwnerRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch (checkedId) {
                    case R.id.yesCarButton:
                        ownerYesButton.setChecked(true);
                        mCarDetailsLayout.setVisibility(View.VISIBLE);
                        break;
                    case R.id.noCarButton:
                        ownerNoButton.setChecked(true);
                        mCarDetailsLayout.setVisibility(View.GONE);
                        break;
                }
            }
        });

        return view;
    }

    /**
     * Retrieves the data inside the widgets and saves it to the database.
     */
    private void saveProfileSettings() {
        final String car = mCar.getText().toString().trim();
        final String registration = mRegistration.getText().toString().trim();
        final String licence = mLicence.getText().toString().trim();
        final int seats = Integer.parseInt(mSeats.getText().toString().trim());
//        Log.d(TAG, "check if function is working: Checking if:already exists.");


        Map<String, Object> hopperUpdates = new HashMap<>();
        hopperUpdates.put("car", car);
        hopperUpdates.put("licence_number", licence);
        hopperUpdates.put("registration_plate", registration);
        hopperUpdates.put("seats", seats);
        hopperUpdates.put("carOwner", true);

        mRef = mFirebaseDatabse.getReference("user").child(userID);
        mRef.updateChildren(hopperUpdates);
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Toast.makeText(getActivity(), "Updated successfully", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }


    private void setProfileWidgets(User userSettings) {
        User user = userSettings;
        mUserSettings = userSettings;
        UniversalImageLoader.setImage(user.getCar_photo(), mCarPhoto, null, "");
        mCar.setText(user.getCar());
        mRegistration.setText(user.getRegistration_plate());
        mLicence.setText(user.getLicence_number());
        mSeats.setText(String.valueOf(user.getSeats()));

        mCarPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: changing profile photo");
                Toast.makeText(getActivity(), "let user upload photo", Toast.LENGTH_SHORT).show();
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLARY_PICK);
            }
        });
    }


    /**
     * --------------------------- Firebase ----------------------------
     **/

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth");

        userID = mAuth.getCurrentUser().getUid();

        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //retrieve user information from the database
                setProfileWidgets(mFirebaseMethods.getUserSettings(dataSnapshot));

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    public void getUserInformation() {
        mRef.child("user").child(userID).child("carOwner").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                carOwner = dataSnapshot.getValue(Boolean.class);
                if (carOwner == false) {
                    ownerNoButton.setChecked(true);
                    mCarDetailsLayout.setVisibility(View.GONE);
                    mCar.setText("");
                    mRegistration.setText("");
                    mLicence.setText("");
                    mSeats.setText("");
                    mCarPhoto.setImageDrawable(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void uploadImage(Uri imageUri) {
        loadingbar.setTitle("saving image");
        loadingbar.setMessage("please wait ....");

        loadingbar.setCanceledOnTouchOutside(true);
        loadingbar.show();

         StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        final StorageReference storRef = storageReference.child("carPicture/" + userID);
        storRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Log.d(TAG, "user profile url is: " + uri.toString());
                                updateCarImage(uri.toString());
                                loadingbar.dismiss();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "on download porfileuri failed" + e.getMessage());
//                                Toast.makeText(LoginActivity.this, "profile picture uploading failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                loadingbar.dismiss();
                            }
                        });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(LoginActivity.this, "profile picture uploading failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                                .getTotalByteCount());
                        Log.d(TAG, "File uploading " + (int) progress + "%");
                    }
                });
        loadingbar.dismiss();

    }

    private void updateCarImage(String imageUri) {
        Map<String, Object> hopperUpdates = new HashMap<>();
        hopperUpdates.put("car_photo", imageUri);

        mRef = mFirebaseDatabse.getReference("user").child(userID);
        mRef.updateChildren(hopperUpdates);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLARY_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageuri = data.getData();

            uploadImage(imageuri);

        }
    }
}
