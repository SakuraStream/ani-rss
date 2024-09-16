package ani.rss.action.play;

import ani.rss.action.BaseAction;
import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.download.BaseDownload;
import ani.rss.entity.Ani;
import ani.rss.entity.PlayItem;
import ani.rss.util.AniUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Auth
@Path("/playlist")
public class PlaylistAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Ani ani = getBody(Ani.class);
        String url = ani.getUrl();
        Optional<Ani> first = AniUtil.ANI_LIST
                .stream()
                .filter(it -> url.equals(it.getUrl()))
                .findFirst();
        if (first.isEmpty()) {
            resultError();
            return;
        }
        ani = first.get();

        List<File> downloadPath = TorrentUtil.getDownloadPath(ani);
        List<PlayItem> collect = downloadPath.stream()
                .flatMap(file -> getPlayItem(file).stream()).collect(Collectors.toList());
        collect = CollUtil.distinct(collect, PlayItem::getTitle, false);
        collect = CollUtil.sort(collect, Comparator.comparingInt(it -> Integer.parseInt(ReUtil.get(s, it.getTitle(), 2))));
        resultSuccess(collect);
    }

    private final String s = "S(\\d+)E(\\d+)";

    public List<PlayItem> getPlayItem(File file) {
        List<PlayItem> playItems = new ArrayList<>();
        if (file.isDirectory()) {
            for (File listFile : ObjectUtil.defaultIfNull(file.listFiles(), new File[0])) {
                playItems.addAll(getPlayItem(listFile));
            }
            return playItems;
        }
        String extName = FileUtil.extName(file);
        if (StrUtil.isBlank(extName)) {
            return playItems;
        }
        if (!BaseDownload.videoFormat.contains(extName)) {
            return playItems;
        }
        if (!ReUtil.contains(s, file.getName())) {
            return playItems;
        }
        PlayItem playItem = new PlayItem();
        playItem.setFilename(Base64.encode(file.getAbsolutePath().replace("\\", "/")))
                .setTitle(ReUtil.get(s, file.getName(), 0));
        playItems.add(playItem);
        return playItems;
    }

}