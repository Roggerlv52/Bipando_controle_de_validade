package com.rogger.bipando.ui.home;

import com.rogger.bipando.data.model.Produto;
import java.util.List;

public interface OnItemClickListener {
    void onItemClick(int position, List<Produto> data);
    void onImageClick(String uri);
}
