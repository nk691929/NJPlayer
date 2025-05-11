package com.example.Music;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> implements Filterable {

    Context context;
    ArrayList<MusicModel> musicList,copyList;
    ArrayList<MusicModel> getUserModelListFilter;
    private OnItemClickListener mListener;
    MusicModel currentSong;

    MediaPlayer myMediaPlayer=MyMediaPlayer.getInstance();

    public MusicAdapter(Context context,ArrayList<MusicModel> musicList,ArrayList<MusicModel> copyList)
    {
        this.context=context;
        this.musicList=musicList;
        this.getUserModelListFilter=musicList;
        this.copyList=copyList;
    }

    //getting filter
    @Override
    public Filter getFilter() {
        Filter filter=new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults=new FilterResults();
                if(charSequence==null|| charSequence.length()==0)
                {
                    filterResults.values=getUserModelListFilter;
                    filterResults.count=getUserModelListFilter.size();
                }
                else {
                    String searchStr=charSequence.toString().toLowerCase();
                    ArrayList<MusicModel> temp=new ArrayList<>();
                    for(MusicModel music:getUserModelListFilter){
                        if(music.getTitle().toLowerCase().contains(searchStr))
                        {
                            temp.add(music);
                        }
                    }

                    filterResults.values=temp;
                    filterResults.count=temp.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                musicList=(ArrayList<MusicModel>) filterResults.values;
                notifyDataSetChanged();
            }
        };
        return filter;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.music_layout,parent,false);
        ViewHolder viewHolder=new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicModel song = musicList.get(position);
        holder.tvTitle.setText(musicList.get(position).title);
        holder.tvDuration.setText(ConvertToMMS(musicList.get(position).getDuration()));
        if (MyMediaPlayer.currentIndex == position) {
            holder.tvTitle.setTextColor(Color.parseColor("#FF2000"));
        } else {
            holder.tvTitle.setTextColor(Color.parseColor("#000000"));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = holder.getAdapterPosition();
                if (MyMediaPlayer.currentIndex != pos) {
                    MyMediaPlayer.getInstance().reset();
                    MyMediaPlayer.currentIndex = pos;

                    try{
                    currentSong = musicList.get(pos);
                    myMediaPlayer.reset();
                    myMediaPlayer.setDataSource(currentSong.getPath());
                    myMediaPlayer.prepare();
                    myMediaPlayer.start();}catch (Exception ignored){}

                    Intent intent = new Intent(context, MusicPlayerActivity.class);
                    intent.putExtra("myList", musicList);
                    intent.putExtra("myCopyList", copyList);
                    intent.putExtra("POS", pos);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    try {
                        if (mListener != null) {
                            mListener.onItemClick(pos);
                        }
                    } catch (Exception ignored) {
                    }
                }else {
                    Intent intent = new Intent(context, MusicPlayerActivity.class);
                    intent.putExtra("myList", musicList);
                    intent.putExtra("myCopyList", copyList);
                    intent.putExtra("POS", pos);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }

        });
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }





    //ViewHolder class
    public class ViewHolder extends RecyclerView.ViewHolder{

        ImageView musicIcon;
        TextView tvTitle,tvDuration;
        RelativeLayout musicHolder;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            musicIcon=itemView.findViewById(R.id.musicIcon);
            tvTitle=itemView.findViewById(R.id.title_text);
            tvDuration=itemView.findViewById(R.id.durationTime);
            musicHolder=itemView.findViewById(R.id.musicHolder);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position) throws IOException;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public String ConvertToMMS(String duration) {
        Long milLies = Long.parseLong(duration);
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milLies) % TimeUnit.HOURS.toMinutes(1)
                , TimeUnit.MILLISECONDS.toSeconds(milLies) % TimeUnit.MINUTES.toSeconds(1));
    }
}
