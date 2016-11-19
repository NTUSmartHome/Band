package com.example.band.app;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


public class LabelListFragment extends ListFragment {
    //original
//    String[] labelList = {"Label:Other", "Sleep", "WatchTV", "Read", "Sweep", "WashDishes",
//            "Exercise", "Walk", "Meal", "PlayPad", "WearShoes", "PlayComputer", "Other", "Reset", "Set"};

    //III   bedroom experiments
    String[] labelList = {"Label:Other", "PlayComputer", "PlayPad", "Read", "Fall", "Sweep", "BendOver", "SquatDown", "StandUp",
            "GetOnBed", "Sleep", "TurnOver", "GetOutOfBed", "Other", "Reset", "Set"};

    //badminton
//    String[] labelList = {"Label:Other", "one", "two", "three", "four", "WashDishes",
//            "Exercise", "Walk", "Meal", "PlayPad", "WearShoes", "WashDishes", "Other", "Reset", "Set"};
    OnSetCurrentLabelListener mSetCurrentLabelCallback;
    private boolean setFlag = false;
    private ArrayAdapter<String> arrayAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
                labelList);
        setListAdapter(arrayAdapter);
    }

    public interface OnSetCurrentLabelListener {
        public void onSetCurrentLabel(String currentLabel);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mSetCurrentLabelCallback = (OnSetCurrentLabelListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    "must implement OnSetCurrentLabelListener");
        }
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        /*Toast.makeText(getActivity(), "Test:" + labelList[position],
                Toast.LENGTH_SHORT).show();*/

        //position is the label position (index)
        switch (position) {
            case 0:
                Toast.makeText(getActivity(), labelList[position],
                        Toast.LENGTH_SHORT).show();
                break;
            //original
//            case 13:
            case 14:
                labelList[0] = "Label:Other";
                mSetCurrentLabelCallback.onSetCurrentLabel(labelList[0]);
                setFlag = false;
                break;
            //original
//            case 14:
            case 15:
                Toast.makeText(getActivity(), "Set Success",
                        Toast.LENGTH_SHORT).show();
                mSetCurrentLabelCallback.onSetCurrentLabel(labelList[0]);
                break;
            default:
                Toast.makeText(getActivity(), labelList[position],
                        Toast.LENGTH_SHORT).show();
                if (!setFlag) {
                    setFlag = true;
                    labelList[0] = "Label:" + labelList[position];
                } else {
                    //if there are over than two activities done at the same time, and need more than one label.
//                    labelList[0] += " " + labelList[position];
                    labelList[0] = "Label:" + labelList[position];
                }
                arrayAdapter.notifyDataSetChanged();
                break;
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        return inflater.inflate(R.layout.activity_main_label_list_fragment, container
                , false);
    }

}
