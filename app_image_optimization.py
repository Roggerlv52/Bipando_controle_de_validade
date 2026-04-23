import os

def optimize_file(path, find_str, replace_str):
    if os.path.exists(path):
        with open(path, 'r') as f:
            content = f.read()
        if find_str in content:
            new_content = content.replace(find_str, replace_str)
            with open(path, 'w') as f:
                f.write(new_content)
            print(f"Optimized: {path}")
        else:
            print(f"Pattern not found in: {path}")

# 1. AdapterHome.java
optimize_file(
    '/home/ubuntu/Bipando_controle_de_validade/app/src/main/java/com/rogger/bp/ui/home/AdapterHome.java',
    '''        Glide.with(context)
                .load(modelo.getImagem())
                .override(350, 350)
                .error(R.drawable.imagem_error)
                .into(holder.imageView);''',
    '''        Glide.with(context)
                .load(modelo.getImagem())
                .override(200, 200)
                .centerCrop()
                .placeholder(R.drawable.carregando)
                .error(R.drawable.imagem_error)
                .into(holder.imageView);'''
)

# 2. AddFragment.java (Fix setImageURI and optimize Glide)
optimize_file(
    '/home/ubuntu/Bipando_controle_de_validade/app/src/main/java/com/rogger/bp/ui/add/AddFragment.java',
    'binding.fragmentImgAdd.setImageURI(Uri.fromFile(photoFile));',
    '''Glide.with(this).load(photoFile).override(500, 500).centerCrop().into(binding.fragmentImgAdd);'''
)
optimize_file(
    '/home/ubuntu/Bipando_controle_de_validade/app/src/main/java/com/rogger/bp/ui/add/AddFragment.java',
    '''            Glide.with(this)
                    .load(imagemUrlGlobal)
                    .placeholder(R.drawable.carregando)
                    .error(R.drawable.imagem_error)
                    .centerCrop()
                    .into(binding.fragmentImgAdd);''',
    '''            Glide.with(this)
                    .load(imagemUrlGlobal)
                    .override(500, 500)
                    .placeholder(R.drawable.carregando)
                    .error(R.drawable.imagem_error)
                    .centerCrop()
                    .into(binding.fragmentImgAdd);'''
)

# 3. EditFragment.java
optimize_file(
    '/home/ubuntu/Bipando_controle_de_validade/app/src/main/java/com/rogger/bp/ui/edit/EditFragment.java',
    '''        Glide.with(requireContext())
                .load(imgUri)
                .override(350, 350)
                .error(R.drawable.up_picture)
                .into(imgUpload);''',
    '''        Glide.with(requireContext())
                .load(imgUri)
                .override(500, 500)
                .centerCrop()
                .placeholder(R.drawable.carregando)
                .error(R.drawable.up_picture)
                .into(imgUpload);'''
)

# 4. ItemDeletedAdapter.java
optimize_file(
    '/home/ubuntu/Bipando_controle_de_validade/app/src/main/java/com/rogger/bp/ui/deleteitem/ItemDeletedAdapter.java',
    '''        Glide.with(context)
                .load(produto.getImagem())
                .override(350, 350)
                .error(R.drawable.imagem_error)
                .into(holder.img_home);''',
    '''        Glide.with(context)
                .load(produto.getImagem())
                .override(200, 200)
                .centerCrop()
                .placeholder(R.drawable.carregando)
                .error(R.drawable.imagem_error)
                .into(holder.img_home);'''
)

# 5. SearchAdapter.kt
optimize_file(
    '/home/ubuntu/Bipando_controle_de_validade/app/src/main/java/com/rogger/bp/ui/search/SearchAdapter.kt',
    '''        Glide.with(context)
            .load(produto.imagem)
            .placeholder(R.drawable.no_picture)
            .error(R.drawable.no_picture)
            .centerCrop()
            .into(holder.imgProduto)''',
    '''        Glide.with(context)
            .load(produto.imagem)
            .override(200, 200)
            .placeholder(R.drawable.no_picture)
            .error(R.drawable.no_picture)
            .centerCrop()
            .into(holder.imgProduto)'''
)

# 6. MainActivity.java
optimize_file(
    '/home/ubuntu/Bipando_controle_de_validade/app/src/main/java/com/rogger/bp/MainActivity.java',
    '''                    .override(200, 200) // reduz tamanho''',
    '''                    .override(100, 100) // reduz tamanho'''
)

# 7. ShowFragment.java
optimize_file(
    '/home/ubuntu/Bipando_controle_de_validade/app/src/main/java/com/rogger/bp/ui/home/ShowFragment.java',
    '''        Glide.with(requireContext())
                .load(imgUri)
                .placeholder(R.drawable.carregando)
                .error(R.drawable.imagem_error)
                .into(imgPreview);''',
    '''        Glide.with(requireContext())
                .load(imgUri)
                .override(1024, 1024)
                .fitCenter()
                .placeholder(R.drawable.carregando)
                .error(R.drawable.imagem_error)
                .into(imgPreview);'''
)
