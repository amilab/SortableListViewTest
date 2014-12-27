package com.example.sortablelistviewtest;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 表示するデータを作成する
        final ArrayList<Item> items = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Item item = new Item();
            item.setTitle("ITEM" + i);
            items.add(item);
        }

        // ListView にアダプタを設定する
        final ItemAdapter adapter = new ItemAdapter(this, R.layout.item, items);
        final SortableListView lv = (SortableListView)findViewById(R.id.list_items);
        lv.setAdapter(adapter);

        // ドラッグに用いる View を指定
        lv.setDraggableViewId(R.id.image_draggable);

        // ListView の並び替えを有効にする
        lv.setSortable(true);

        // ListView のアイテムのドラッグを監視するリスナーを設定
        lv.setDragListener(new SortableListView.SimpleDragListener() {
            @Override
            public boolean onStopDrag(int positionFrom, int positionTo) {
                if (positionFrom < 0 || positionTo < 0 || positionFrom == positionTo) {
                    return false;
                }

                // アイテムを入れ替える
                final Item item = adapter.getItem(positionFrom);
                adapter.remove(item);
                adapter.insert(item, positionTo);

                // ListView の再表示
                lv.invalidateViews();

                // 並び順の保存
                // .....

                return super.onStopDrag(positionFrom, positionTo);
            }
        });
    }
}
