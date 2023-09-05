if [ ! -x native-image ]; then
    echo "> native-image not found"
    echo "  Please intall GraalVM native-image component, or make"
    echo "  a link to the native-image binary, for example:"
    echo "  ln -sv /usr/lib/jvm/java-19-graalvm/bin/native-image native-image"
    exit
fi

if [ ! -d '.nu-build' ] && ! mkdir '.nu-build'; then
    echo ".nu-build does not exist"
    exit
fi

cd .nu-build

if [ ! -d 'logs' ] && ! mkdir 'logs'; then
    echo ".nu-build/logs does not exist"
    exit
fi

echo "Compiling ./jpcode"
echo "FAIL: finish the script :)"
cd -

