package org.metaeffekt.core.maven.inventory.linkres;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LinuxSymlinkResolverTest {

    @Test
    public void resolveValidCycle0() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "/iamtarget");
        symlinks.put("/iamtarget/iamlink", "/");
        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/file.txt");

        assertEquals(ResolverStatus.DONE, holder.getStatus());
        assertEquals("/iamtarget/file.txt", holder.getCurrentPath());
    }

    @Test
    public void resolveValidCycle1() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "iamtarget");
        symlinks.put("/iamtarget/iamlink", "../");
        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/file.txt");

        assertEquals(ResolverStatus.DONE, holder.getStatus());
    }

    @Test
    public void resolveValidCycle2() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "/iamtarget");
        symlinks.put("/iamtarget/iamlink", "/iamtarget");
        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/file.txt");

        assertEquals(ResolverStatus.DONE, holder.getStatus());
        assertEquals("/iamtarget/file.txt", holder.getCurrentPath());
    }

    @Test
    public void resolveValidCycle3() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "/iamtarget");
        symlinks.put("/iamtarget/iamlink", "/iamlink");
        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/file.txt");

        assertEquals(ResolverStatus.DONE, holder.getStatus());
        assertEquals("/iamtarget/file.txt", holder.getCurrentPath());
    }

    @Test
    public void resolveBadTraversal() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "/iamtarget");
        symlinks.put("/iamtarget/iamlink", "../../");
        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/iamlink/file.txt");

        assertEquals(ResolverStatus.BAD_TRAVERSAL, holder.getStatus());
    }

    @Test
    public void resolveInvalidCycleIndirection0() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "/iamlink");

        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/file.txt");

        assertEquals(ResolverStatus.CYCLIC, holder.getStatus());
    }

    @Test
    public void resolveInvalidCycleIndirection1() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "/iamtarget/iamlink");
        symlinks.put("/iamtarget/iamlink", "/iamlink");

        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/file.txt");

        assertEquals(ResolverStatus.CYCLIC, holder.getStatus());
    }

    @Test
    public void resolveInvalidCycleIndirection1Alt() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "/iamtarget/iamlink");
        symlinks.put("/iamtarget/iamlink", "/iamlink");

        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/iamlink/iamlink/file.txt");

        assertEquals(ResolverStatus.CYCLIC, holder.getStatus());
    }

    @Test
    public void resolveInvalidCycleIndirection2Alt() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/iamlink", "/iamtarget/iamlink");
        symlinks.put("/iamtarget/iamlink", "/iamtarget/iamanotherlink");
        symlinks.put("/iamtarget/iamanotherlink", "/iamlink");

        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/iamlink/file.txt");

        assertEquals(ResolverStatus.CYCLIC, holder.getStatus());
    }

    @Test
    public void resolveActualExample() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/sbin", "/usr/sbin");

        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/sbin/ldconfig");

        assertEquals(ResolverStatus.DONE, holder.getStatus());
        assertEquals("/usr/sbin/ldconfig", holder.getCurrentPath());
    }

    @Test
    public void resolveActualExampleRelative() {
        Map<String, String> symlinks = new HashMap<>();

        symlinks.put("/sbin", "usr/sbin");

        LinuxSymlinkResolver resolver = new LinuxSymlinkResolver(symlinks);

        ResolverPathHolder holder = resolver.resolve("/sbin/ldconfig");

        assertEquals(ResolverStatus.DONE, holder.getStatus());
        assertEquals("/usr/sbin/ldconfig", holder.getCurrentPath());
    }
}
