Google Docs File System (gdocsfs) provides a mountable Linux filesystem which uses your Google Docs account as its storage medium. GDocsFS is a Java application and uses the [FUSE](http://fuse.sourceforge.net/) userland filesystem infrastructure to help provide the filesystem, and [Google Data APIs](http://code.google.com/apis/gdata/) to communicate with Google Docs.

### Installing: ###
```
$ unzip gdocsfs-x.y.z.zip
$ cd gdocsfs
$ sudo ./install /path/to/install
```
_/path/to/install is optional. Default is /usr/lib._


### Configuring google account: ###
```
$ sudo vim /path/to/gdocsfs/conf/gdocsfs.properties
```
_Edit your username. The password is optional._


### Running: ###
GDocsFS filesystems is mounted with the regular `mount` command, or even to be listed in `/etc/fstab`
  1. `gdocsfsmount /path/to/gdocsfs/home /path/of/mount/point [options]`
  1. in `/etc/fstab`, add:
    * `/path/to/gdocsfs/home /path/of/mount/point gdocsfs noauto[,options]`