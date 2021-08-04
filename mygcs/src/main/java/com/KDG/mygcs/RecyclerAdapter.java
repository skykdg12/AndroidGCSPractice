package com.KDG.mygcs;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private List<String> itemList=new ArrayList<>();

    public RecyclerAdapter(ArrayList<String> msgs){

        for(int i = 0; i < msgs.size();i++){
            itemList.add(msgs.get(i));
        }

        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        private TextView item;
        public ViewHolder(View itemView){
            super(itemView);
            item=(TextView)itemView.findViewById(R.id.RecyclerViewItem);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view= LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recyclerview_item,viewGroup,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int i) {
        ViewHolder holder=(ViewHolder)viewHolder;

        holder.item.setText(itemList.get(i));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

}