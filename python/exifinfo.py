# coding=utf-8
from PIL import Image
from PIL.ExifTags import TAGS
import os


def findAllFiles(root_dir, filter):
    """
    遍历搜索文件

    :param root_dir:搜索目录
    :param filter: 搜索文件类型
    :return: 路径、文件名、路径+文件名
    """
    print("Finding files ends with \'" + filter + "\' ...")
    separator = os.path.sep
    paths = []
    names = []
    files = []
    # 遍历
    for parent, dirname, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith(filter):
                paths.append(parent + separator)
                names.append(filename)
    for i in range(paths.__len__()):
        files.append(paths[i] + names[i])
    print (names.__len__().__str__() + " files have been found.")
    paths.sort()
    names.sort()
    files.sort()
    return paths, names, files


def get_exif_data(fname):
    """
    获取EXIF信息

    :param fname: 影像文件路径
    :return: 字典类型的EXIF信息
    """
    ret = {}
    try:
        img = Image.open(fname)
        if hasattr(img, '_getexif'):
            exifinfo = img._getexif()
            if exifinfo != None:
                for tag, value in exifinfo.items():
                    decoded = TAGS.get(tag, tag)
                    ret[decoded] = value
    except IOError:
        print 'IOERROR ' + fname
    return ret


def decodeGPS(gps_info):
    """
    从结构数据中解析经纬度信息

    :param gps_info: 结构化数据
    :return: 经纬度数据
    """
    latFlag = gps_info[0][1]
    lat = gps_info[1][1]
    lat_deg = lat[0][0]
    lat_min = lat[1][0]
    lat_sec = lat[2][0] / 10000.0
    lonFlag = gps_info[2][1]
    lon = gps_info[3][1]
    lon_deg = lon[0][0]
    lon_min = lon[1][0]
    lon_sec = lon[2][0] / 10000.0
    lat_decimal = lat_deg + lat_min / 60.0 + lat_sec / 3600.0
    lon_decimal = lon_deg + lon_min / 60.0 + lon_sec / 2600.0
    lat_info = (latFlag, lat_decimal)
    lon_info = (lonFlag, lon_decimal)
    return lat_info, lon_info


def decode2Str(loc_info):
    """
    将位置信息转成字符串

    :param loc_info: 结构化位置信息
    :return: 字符串位置信息，如32.12345N,117.12345E
    """
    lat = loc_info[0][1].__str__() + loc_info[0][0]
    lon = loc_info[1][1].__str__() + loc_info[1][0]
    str = lat + "," + lon
    return str


def getGPSInfo(filename):
    """
    获取EXIF中的地理位置信息

    :param filename: 影像路径
    :return: 字符串类型的地理位置信息
    """

    exif = get_exif_data(filename)
    if exif.has_key('GPSInfo'):
        info = exif.get('GPSInfo')
        gps_info = info.items()
        if len(gps_info) < 4:
            return "No GPSInfo."
        else:
            loc = decodeGPS(gps_info)
            return decode2Str(loc)
    else:
        return "No GPSInfo."


def getGPSInfoBatch(filenames):
    """
    批量获取文件的位置信息

    :param filenames: list类型的文件列表
    :return: list类型的位置列表
    """
    GPSInfos = []
    for filename in filenames:
        res = getGPSInfo(filename)
        if res.__contains__('No GPSInfo'):
            continue
        else:
            GPSInfos.append(res)
    return GPSInfos


paths, names, files = findAllFiles('E:\\Camera', 'jpg')
GPS = getGPSInfoBatch(files)
for item in GPS:
    print item
