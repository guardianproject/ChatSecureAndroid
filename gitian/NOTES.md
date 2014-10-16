Gitian instructions for ChatSecure
==================================

Setup:

* Use a recent version of Ubuntu (might work on anything that can run vmbuilder and LXC)
* `sudo apt-get install apt-cacher python-vm-builder lxc`
* `sudo service apt-cacher start`
* `sudo addgroup lxc`
* Add the following to /etc/sudoers:
  `%lxc ALL=NOPASSWD: /usr/bin/lxc-start`
  and add yourself to the lxc group.
* `git clone https://github.com/devrandom/gitian-builder`
* `git clone https://github.com/guardianproject/ChatSecureAndroid`
* `bin/make-base-vm --arch i386 --lxc`
  (requires sudo to extract a partition from resulting image)
* `cd gitian-builder`
* Download sdk 20.0.1 to ../android-sdk-linux
* `../android-sdk-linux/tools/android sdk`
    * Download Tools/Platform-tools
    * Download API 15 and API 4 SDK Platform and Google APIs
* `tar -czf inputs/android-sdk-linuz.tgz ../android-sdk-linux`

Build:

Every time you boot:

    sudo brctl addbr br0
    sudo ifconfig br0 10.0.2.2/24 up

and then:

* `export USE_LXC=1`
* `bin/gbuild -i --commit ChatSecure=COMMIT ../ChatSecure/gitian/chatsecure.yml`
  where COMMIT is a tag, branch or commit (latter recommended for repeatability)

Optional - poke around the target environment:

    export LXC_SUITE=lucid LXC_ARCH=i386
    PATH=libexec:$PATH
    on-target
    stty echo

