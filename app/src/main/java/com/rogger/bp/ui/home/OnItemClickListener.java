package com.rogger.bp.ui.home;

import com.rogger.bp.data.model.Produto;
import java.util.List;

public interface OnItemClickListener {
    void onItemClick(int position, List<Produto> data);
    void onImageClick(String uri);
}
