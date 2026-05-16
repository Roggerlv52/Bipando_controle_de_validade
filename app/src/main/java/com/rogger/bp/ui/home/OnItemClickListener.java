package com.rogger.bp.ui.home;

import com.rogger.bp.data.model.PostProduct;
import com.rogger.bp.data.model.Produto;
import java.util.List;

public interface OnItemClickListener {
    void onItemClick(int position, List<PostProduct> data);
    void onImageClick(String uri);
}
