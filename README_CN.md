# EasyAlbum

EasyAlbum是一个简单易用的相册库。

## 一. 特性
EasyAlbum的特性包括单不限于以下列表：

- 支持图片/视频预览；
- 支持自定义目录排序；
- 支持自定义筛选条件；
- 支持单选/多选；
- 支持显示选择顺序和限定选择数量；
- 支持“原图”选项；
- 支持再次进入相册时传入已经选中的图片/视频；
- 支持切换出APP外拍照或删除照片后，回到相册时自动刷新；
- 内部实现缓存，再次进入相册秒开；
- 支持预加载，提升首次打开的显示速度。

![](images/easy_album_cn.jpg)

## 二. 使用方法

### 2.1 下载
```gradle
implementation 'io.github.billywei01:easyalbum:1.1.6'
```

### 2.2 全局配置

```kotlin
EasyAlbum.config()
    .setImageLoader(GlideImageLoader)
    .setDefaultFolderComparator { o1, o2 -> o1.name.compareTo(o2.name)}
    .setItemAnimator(DefaultItemAnimator())
```

除了ImageLoader必须设置之外，其他的配置都是可选项。


### 2.3 启动相册

```kotlin
EasyAlbum.from(this)
    .setFilter(TestMediaFilter(option))
    .setSelectedLimit(selectLimit)
    .setOverLimitCallback(overLimitCallback)
    .setSelectedList(mediaAdapter?.getData())
    .setAllString(option.text)
    .enableOriginal()
    .start { result ->
        mediaAdapter?.setData(result.selectedList)
    }
```

EasyAlbum启动相册页面以from起头，以start结束。

## 三、相关链接

https://juejin.cn/post/7215163152907092024


## License
See the [LICENSE](LICENSE) file for license rights and limitations.

