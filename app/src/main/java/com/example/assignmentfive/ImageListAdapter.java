package com.example.assignmentfive;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
// https://developer.android.com/reference/android/widget/CompoundButton#setOnCheckedChangeListener(android.widget.CompoundButton.OnCheckedChangeListener)
public class ImageListAdapter extends ArrayAdapter<ImageItem> {
    private ArrayList<ImageItem> dataList;
    ImageListAdapter(Context context, int resource, ArrayList<ImageItem> objects) {
        super(context, resource, objects);
        dataList = objects;
    }

    public interface ItemCheckListener { void itemChanged(); } // Change listener
    public void setItemCheckListener(ItemCheckListener itemCheckListener) { //Register a callback to be invoked when the checked state of this button changes.
        this.itemCheckListener = itemCheckListener;
    }
    private ItemCheckListener itemCheckListener;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        ImageItem currentItem = getItem(position);
        ImageView currentImage = convertView.findViewById(R.id.imageinlist);
        TextView currentName = convertView.findViewById(R.id.textinlist);

        CheckBox includecheckbox = convertView.findViewById(R.id.checkbox);

        if (currentItem.getImageResource() != 0) {
            //old way
            currentImage.setImageResource(currentItem.getImageResource());
        } else if (currentItem.getImageBitmap() != null) {
            // new way
            currentImage.setImageBitmap(currentItem.getImageBitmap());
        }

        currentName.setText(currentItem.getTagText());

        // Compound button method for when item is checked/unchecked
        includecheckbox.setChecked(currentItem.isChecked());
        includecheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { //Changes the checked state of this button
                currentItem.setChecked(isChecked);
                if (itemCheckListener != null) {
                    itemCheckListener.itemChanged();
                }
            }
        });
        return convertView;
    }

    public void updateData(ArrayList<ImageItem> recentData) {
        dataList.clear();
        dataList.addAll(recentData);
        notifyDataSetChanged();
    }

    public ArrayList<ImageItem> getDataList() {
        return dataList;
    }
}